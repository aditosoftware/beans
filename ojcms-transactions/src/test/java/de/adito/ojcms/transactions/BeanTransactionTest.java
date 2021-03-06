package de.adito.ojcms.transactions;

import de.adito.ojcms.beans.IBean;
import de.adito.ojcms.beans.literals.fields.IField;
import de.adito.ojcms.cdi.*;
import de.adito.ojcms.transactions.annotations.TransactionalScoped;
import de.adito.ojcms.transactions.api.*;
import de.adito.ojcms.transactions.exceptions.ConcurrentTransactionException;
import de.adito.ojcms.transactions.spi.*;
import org.jboss.weld.proxy.WeldClientProxy;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Black box test for {@link ITransaction}.
 *
 * @author Simon Danner, 29.12.2019
 */
public class BeanTransactionTest extends AbstractCdiTest
{
  private static final String CONTAINER_ID = "containerId";
  private static final SingleBeanKey SINGLE_BEAN_KEY = new SingleBeanKey("singleBeanId");
  private static final int CONTAINER_SIZE = 7;
  @SuppressWarnings("unchecked")
  private static final IField<Integer> BEAN_FIELD = Mockito.mock(IField.class);
  private static final int BEAN_VALUE = 42;

  @Inject
  private ICdiControl cdiControl;
  @Inject
  private TransactionManager transactionManager;
  @Inject
  private ITransaction transaction;
  @Inject
  private IBeanDataStorage beanDataStorage;

  private IBeanDataStorage beanDataStorageMock;

  @BeforeEach
  public void setup()
  {
    beanDataStorageMock = (IBeanDataStorage) ((WeldClientProxy) beanDataStorage).getMetadata().getContextualInstance();
    reset(beanDataStorageMock);
  }

  @Test
  public void testContainerSizeRequest()
  {
    assertEquals(CONTAINER_SIZE, transaction.requestContainerSize(CONTAINER_ID));
  }

  @Test
  public void testContainerBeanDataRequestByIndex()
  {
    final CurrentIndexKey key = new CurrentIndexKey(CONTAINER_ID, 0);
    final PersistentBeanData beanData = transaction.requestBeanDataByIndex(key);

    assertEquals(0, beanData.getIndex());
    _checkBeanData(beanData);
  }

  @Test
  public void testContainerBeanDataRequestByIdentifiers()
  {
    final PersistentBeanData beanData = transaction.requestBeanDataByIdentifierTuples(CONTAINER_ID, Collections.emptyMap()) //
        .orElseThrow(AssertionError::new);
    _checkBeanData(beanData);
  }

  @Test
  public void testSingleBeanDataRequest()
  {
    final PersistentBeanData beanData = transaction.requestSingleBeanData(SINGLE_BEAN_KEY);
    _checkBeanData(beanData);
  }

  @Test
  public void testRequestBeanTypeWithinContainer_Added_Fails()
  {
    final CurrentIndexKey key = new CurrentIndexKey(CONTAINER_ID, 0);
    _registerBeanAddition(0);
    assertThrows(IllegalStateException.class, () -> transaction.requestBeanTypeWithinContainer(key));
  }

  @Test
  public void testRequestBeanTypeWithinContainer_Removed_Fails()
  {
    transaction.registerBeanRemoval(new CurrentIndexKey(CONTAINER_ID, 4));
    transaction.registerBeanRemoval(new CurrentIndexKey(CONTAINER_ID, 1));
    assertThrows(IllegalStateException.class, () -> transaction.requestBeanTypeWithinContainer(new CurrentIndexKey(CONTAINER_ID, 3)));
  }

  @Test
  public void testRequestBeanTypeWithinContainer()
  {
    final CurrentIndexKey key = new CurrentIndexKey(CONTAINER_ID, 0);
    assertEquals(IBean.class, transaction.requestBeanTypeWithinContainer(key));
  }

  @Test
  public void testRequestContainerSizeAfterChangeInSameTransaction()
  {
    _registerBeanAddition(1);
    _registerBeanAddition(2);
    transaction.registerBeanRemoval(new CurrentIndexKey(CONTAINER_ID, 0));

    final int size = transaction.requestContainerSize(CONTAINER_ID);
    assertEquals(CONTAINER_SIZE + 1, size);
  }

  @Test
  public void testRequestValueAfterChangeInSameTransaction()
  {
    final CurrentIndexKey key = new CurrentIndexKey(CONTAINER_ID, 0);
    transaction.registerContainerBeanValueChange(key, BEAN_FIELD, 100);
    final Object value = transaction.requestBeanDataByIndex(key).getData().get(BEAN_FIELD);
    assertEquals(100, value);
  }

  @Test
  public void testCommitChanges()
  {
    _registerChanges();
    transactionManager.commitChanges();

    verify(beanDataStorageMock).processAdditionsForContainer(any(), any());
    verify(beanDataStorageMock).processRemovals(any());
    verify(beanDataStorageMock).processChangesForContainerBean(any(), any());
    verify(beanDataStorageMock).processChangesForSingleBean(any(), any());
  }

  @Test
  public void testRollback()
  {
    _registerChanges();
    transactionManager.rollbackChanges();
    verify(beanDataStorageMock).rollbackChanges();
  }

  @Test
  public void testAvoidConcurrentModificationMultipleTransactions()
  {
    _registerBeanAddition(1);
    cdiControl.startContext(TransactionalScoped.class);
    assertThrows(ConcurrentTransactionException.class, () -> transaction.requestContainerSize(CONTAINER_ID));
  }

  @Test
  public void testConcurrentModificationOkayInSameTransaction()
  {
    transaction.registerSingleBeanValueChange(SINGLE_BEAN_KEY, BEAN_FIELD, 12);
    final PersistentBeanData beanData = transaction.requestSingleBeanData(SINGLE_BEAN_KEY);
    assertNotNull(beanData);
  }

  private void _checkBeanData(PersistentBeanData pResult)
  {
    assertEquals(1, pResult.getData().size());
    final Map.Entry<IField<?>, Object> firstEntry = pResult.getData().entrySet().iterator().next();
    assertSame(BEAN_FIELD, firstEntry.getKey());
    assertEquals(BEAN_VALUE, firstEntry.getValue());
  }

  private void _registerChanges()
  {
    _registerBeanAddition(1);
    transaction.registerBeanRemoval(new CurrentIndexKey(CONTAINER_ID, 1));
    transaction.registerContainerBeanValueChange(new CurrentIndexKey(CONTAINER_ID, 0), BEAN_FIELD, 6);
    transaction.registerSingleBeanValueChange(SINGLE_BEAN_KEY, BEAN_FIELD, 7);
  }

  private void _registerBeanAddition(int pIndex)
  {
    transaction.registerBeanAddition(new BeanAddition(pIndex, Collections.emptyMap(), IBean.class, CONTAINER_ID));
  }

  @Alternative
  @Priority(100)
  static class MockedBeanLoader implements IBeanDataLoader
  {
    private static final Map<IField<?>, Object> BEAN_DATA = Collections.singletonMap(BEAN_FIELD, BEAN_VALUE);

    @Override
    public int loadContainerSize(String pContainerId)
    {
      return CONTAINER_SIZE;
    }

    @Override
    public PersistentBeanData loadContainerBeanDataByIndex(InitialIndexKey pKey)
    {
      return new PersistentBeanData(0, BEAN_DATA);
    }

    @Override
    public <BEAN extends IBean> Class<BEAN> loadBeanTypeWithinContainer(InitialIndexKey pKey)
    {
      //noinspection unchecked
      return (Class<BEAN>) IBean.class;
    }

    @Override
    public Optional<PersistentBeanData> loadContainerBeanDataByIdentifiers(String pContainerId, Map<IField<?>, Object> pIdentifiers)
    {
      return Optional.of(new PersistentBeanData(0, BEAN_DATA));
    }

    @Override
    public PersistentBeanData loadSingleBeanData(SingleBeanKey pKey)
    {
      return new PersistentBeanData(0, BEAN_DATA);
    }

    @Override
    public Map<Integer, PersistentBeanData> fullContainerLoad(String pContainerId)
    {
      return Collections.singletonMap(0, loadContainerBeanDataByIndex(null));
    }
  }
}
