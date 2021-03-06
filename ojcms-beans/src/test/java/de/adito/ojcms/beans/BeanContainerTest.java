package de.adito.ojcms.beans;

import de.adito.ojcms.beans.annotations.Identifier;
import de.adito.ojcms.beans.base.IEqualsHashCodeChecker;
import de.adito.ojcms.beans.exceptions.OJRuntimeException;
import de.adito.ojcms.beans.exceptions.container.BeanContainerLimitReachedException;
import de.adito.ojcms.beans.literals.fields.types.*;
import de.adito.ojcms.beans.literals.fields.util.FieldValueTuple;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IBeanContainer}.
 *
 * @author Simon Danner, 28.07.2018
 */
class BeanContainerTest
{
  private IBeanContainer<SomeBean> container;

  @BeforeEach
  public void init()
  {
    container = IBeanContainer.empty(SomeBean.class);
  }

  @Test
  public void testBeanType()
  {
    assertSame(SomeBean.class, container.getBeanType());
  }

  @Test
  public void testAddition()
  {
    final SomeBean bean = new SomeBean();
    container.addBean(bean);
    assertEquals(1, container.size());
    assertSame(bean, container.getBean(0));
    container.addBean(bean); //Duplicate should be ok
    assertEquals(2, container.size());
  }

  @Test
  public void testAdditionAtBadIndex()
  {
    final SomeBean bean = new SomeBean();
    assertThrows(IndexOutOfBoundsException.class, () -> container.addBean(bean, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> container.addBean(bean, 1));
  }

  @Test
  public void testAdditionAtIndex()
  {
    final SomeBean bean1 = new SomeBean();
    final SomeBean bean2 = new SomeBean();
    container.addBean(bean1);
    container.addBean(bean2, 0);
    assertEquals(2, container.size());
    assertSame(bean2, container.getBean(0));
  }

  @Test
  public void testMultipleAddition()
  {
    container.addMultiple(Arrays.asList(new SomeBean(), new SomeBean(), new SomeBean()));
    assertEquals(3, container.size());
  }

  @Test
  public void testMerge()
  {
    final IBeanContainer<SomeBean> container1 = IBeanContainer.ofIterableNotEmpty(
        Arrays.asList(new SomeBean(), new SomeBean(), new SomeBean()));
    final IBeanContainer<SomeBean> container2 = IBeanContainer.ofVariableNotEmpty(new SomeBean(), new SomeBean());
    container1.merge(container2);
    assertEquals(5, container1.size());
  }

  @Test
  public void testReplacementAtBadIndex()
  {
    final SomeBean bean = new SomeBean();
    assertThrows(IndexOutOfBoundsException.class, () -> container.replaceBean(bean, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> container.replaceBean(bean, 1));
  }

  @Test
  public void testReplacement()
  {
    final SomeBean bean1 = new SomeBean();
    final SomeBean bean2 = new SomeBean();
    container.addBean(bean1);
    container.replaceBean(bean2, 0);
    assertEquals(1, container.size());
    assertSame(bean2, container.getBean(0));
  }

  @Test
  public void testRemoval()
  {
    final SomeBean bean = new SomeBean();
    container.addBean(bean);
    assertTrue(container.removeBean(bean));
    assertTrue(container.isEmpty());
    container.addBean(bean);
    assertSame(bean, container.removeBean(0));
    assertTrue(container.isEmpty());
  }

  @Test
  public void testRemoveIf()
  {
    final SomeBean bean1 = new SomeBean();
    final SomeBean bean2 = new SomeBean();
    bean1.setValue(SomeBean.SOME_FIELD, 0);
    bean2.setValue(SomeBean.SOME_FIELD, 1);
    container.addMultiple(Arrays.asList(bean1, bean2));
    container.removeBeanIf(pBean -> pBean.getValue(SomeBean.SOME_FIELD) == 0);
    assertEquals(1, container.size());
    assertSame(bean2, container.getBean(0));
  }

  @Test
  public void testRemoveIfAndBreak()
  {
    final IBeanContainer<SomeBean> container = IBeanContainer.ofVariableNotEmpty(new SomeBean(), new SomeBean(), new SomeBean());
    container.removeBeanIfAndBreak(pBean -> true);
    assertEquals(2, container.size());
  }

  @Test
  public void testGetBeanBadIndex()
  {
    container.addBean(new SomeBean());
    assertThrows(IndexOutOfBoundsException.class, () -> container.getBean(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> container.getBean(1));
  }

  @Test
  public void testGetBean()
  {
    final SomeBean bean = new SomeBean();
    container.addBean(bean);
    assertSame(bean, container.getBean(0));
    container.addBean(new SomeBean());
    assertSame(bean, container.getBean(0));
  }

  @Test
  public void testIndexOf()
  {
    final SomeBean bean = new SomeBean();
    container.addBean(bean);
    assertEquals(0, container.indexOf(bean));
    container.addBean(new SomeBean());
    assertEquals(0, container.indexOf(bean)); //Initial bean should still be the first
    //Test behaviour for equals
    assertEquals(-1, container.indexOf(new SomeBean(50)));
    assertEquals(0, container.indexOf(new SomeBean()));
  }

  @Test
  public void testClear()
  {
    final IBeanContainer<SomeBean> container = IBeanContainer.ofVariableNotEmpty(new SomeBean(), new SomeBean(), new SomeBean());
    container.clear();
    assertTrue(container.isEmpty());
  }

  @Test
  public void testContains()
  {
    final SomeBean bean = new SomeBean(42);
    container.addBean(bean);
    assertTrue(container.contains(bean));
    //Test behaviour for equals
    final SomeBean anotherBean = new SomeBean(43);
    assertFalse(container.contains(anotherBean));
    anotherBean.setValue(SomeBean.SOME_FIELD, 42);
    assertTrue(container.contains(anotherBean));
    anotherBean.setValue(SomeBean.ANOTHER_FIELD, "differentValue");
    assertTrue(container.contains(anotherBean));
  }

  @Test
  public void testSort()
  {
    final Stream<SomeBean> stream = IntStream.range(0, 5).mapToObj(SomeBean::new);
    final IBeanContainer<SomeBean> container = IBeanContainer.ofStreamNotEmpty(stream);
    container.sort(Comparator.reverseOrder());
    IntStream.range(0, 5).forEach(pIndex -> assertSame(4 - pIndex, container.getBean(pIndex).getValue(SomeBean.SOME_FIELD)));
  }

  @Test
  public void testWithLimitNotEvicting()
  {
    container.withLimit(1, false);
    container.addBean(new SomeBean());
    assertThrows(BeanContainerLimitReachedException.class, () -> container.addBean(new SomeBean()));
  }

  @Test
  public void testWithLimitEvicting()
  {
    final SomeBean bean = new SomeBean();
    container.withLimit(1, true);
    container.addBean(new SomeBean());
    container.addBean(bean);
    assertEquals(1, container.size());
    assertSame(bean, container.getBean(0));
  }

  @Test
  public void testWithLimitExceed()
  {
    final SomeBean bean = new SomeBean();
    container.addBean(new SomeBean());
    container.addBean(bean);
    container.withLimit(1, false);
    assertEquals(1, container.size());
    assertSame(bean, container.getBean(0));
  }

  @Test
  public void testGetDistinctValues()
  {
    final SomeBean bean1 = new SomeBean(1);
    final SomeBean bean2 = new SomeBean(2);
    final SomeBean bean3 = new SomeBean(1);
    container.addMultiple(Arrays.asList(bean1, bean2, bean3));
    final Set<Integer> distinctValues = container.getDistinctValuesFromField(SomeBean.SOME_FIELD);
    assertEquals(2, distinctValues.size());
    assertTrue(distinctValues.contains(1));
    assertTrue(distinctValues.contains(2));
  }

  @Test
  public void testEqualsAndHashCode()
  {
    final IBeanContainer<SomeBean> otherContainer = IBeanContainer.empty(SomeBean.class);
    final IEqualsHashCodeChecker equalsHashCodeChecker = IEqualsHashCodeChecker.create(container, otherContainer);
    equalsHashCodeChecker.makeAssertion(true);
    container.addBean(new SomeBean());
    equalsHashCodeChecker.makeAssertion(false);
    otherContainer.addBean(new SomeBean());
    equalsHashCodeChecker.makeAssertion(true);

    IntStream.range(0, 5).forEach(pIndex ->
    {
      container.addBean(new SomeBean());
      otherContainer.addBean(new SomeBean());
    });

    equalsHashCodeChecker.makeAssertion(true);
    container.getBean(3).setValue(SomeBean.SOME_FIELD, 9999);
    equalsHashCodeChecker.makeAssertion(false);
  }

  @Test
  public void testFindOneByFieldValue_MoreThanOneResult()
  {
    container.addBean(new SomeBean(1));
    container.addBean(new SomeBean(1));

    assertThrows(OJRuntimeException.class, () -> container.findOneByFieldValue(SomeBean.SOME_FIELD, 1));
  }

  @Test
  public void testFindOneByFieldValue()
  {
    final Optional<SomeBean> resultForEmptyContainer = container.findOneByFieldValue(SomeBean.SOME_FIELD, 0);
    assertFalse(resultForEmptyContainer.isPresent());

    final SomeBean firstBean = new SomeBean(0);
    container.addBean(firstBean);
    container.addBean(new SomeBean(1));
    container.addBean(new SomeBean(2));

    final Optional<SomeBean> result = container.findOneByFieldValue(SomeBean.SOME_FIELD, 0);
    assertTrue(result.isPresent());
    assertSame(firstBean, result.get());

    final Optional<SomeBean> resultEmpty = container.findOneByFieldValue(SomeBean.SOME_FIELD, 3);
    assertFalse(resultEmpty.isPresent());
  }

  @Test
  public void testFindByFieldValue()
  {
    final List<SomeBean> resultForEmptyContainer = container.findByFieldValue(SomeBean.SOME_FIELD, 0);
    assertTrue(resultForEmptyContainer.isEmpty());

    final SomeBean firstBean = new SomeBean(0);
    final SomeBean thirdBean = new SomeBean(0);
    container.addBean(firstBean);
    container.addBean(new SomeBean(1));
    container.addBean(thirdBean);

    final List<SomeBean> result = container.findByFieldValue(SomeBean.SOME_FIELD, 0);
    assertEquals(2, result.size());
    assertSame(firstBean, result.get(0));
    assertSame(thirdBean, result.get(1));
  }

  @Test
  public void findOneByFieldValues()
  {
    final Optional<SomeBean> resultEmptyContainer = container.findOneByFieldValues();
    assertFalse(resultEmptyContainer.isPresent());

    container.addBean(new SomeBean(0));
    container.addBean(new SomeBean(1));

    final Optional<SomeBean> resultNoTuple = container.findOneByFieldValues();
    assertFalse(resultNoTuple.isPresent());

    final Optional<SomeBean> oneResult = container.findOneByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 0));
    assertTrue(oneResult.isPresent());
    assertSame(container.getBean(0), oneResult.get());

    final Optional<SomeBean> noResult = container.findOneByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 7));
    assertFalse(noResult.isPresent());

    final Optional<SomeBean> oneResultTwoTuples = container.findOneByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 1),
        new FieldValueTuple<>(SomeBean.ANOTHER_FIELD, "anotherValue"));

    assertTrue(oneResultTwoTuples.isPresent());
    assertSame(container.getBean(1), oneResultTwoTuples.get());

    assertThrows(OJRuntimeException.class,
        () -> container.findOneByFieldValues(new FieldValueTuple<>(SomeBean.ANOTHER_FIELD, "anotherValue")));
  }

  @Test
  public void testFindByFieldValues()
  {
    final List<SomeBean> resultEmptyContainerNoTuple = container.findByFieldValues();
    assertTrue(resultEmptyContainerNoTuple.isEmpty());

    final SomeBean firstBean = new SomeBean(0);
    final SomeBean secondBean = new SomeBean(2);
    secondBean.setValue(SomeBean.ANOTHER_FIELD, "testValue");
    container.addBean(firstBean);
    container.addBean(secondBean);

    final List<SomeBean> resultNoTuple = container.findByFieldValues();
    assertTrue(resultNoTuple.isEmpty());

    final List<SomeBean> resultOneTuple = container.findByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 0));
    assertEquals(1, resultOneTuple.size());
    assertSame(firstBean, resultOneTuple.get(0));

    final List<SomeBean> badResultOneTuple = container.findByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 1));
    assertTrue(badResultOneTuple.isEmpty());


    final List<SomeBean> resultTwoTuple = container.findByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 2),
        new FieldValueTuple<>(SomeBean.ANOTHER_FIELD, "testValue"));
    assertEquals(1, resultOneTuple.size());
    assertSame(secondBean, resultTwoTuple.get(0));

    final List<SomeBean> badResultTwoTuple = container.findByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 2),
        new FieldValueTuple<>(SomeBean.ANOTHER_FIELD, "anotherValue"));
    assertTrue(badResultTwoTuple.isEmpty());

    final SomeBean thirdBean = new SomeBean(0);
    container.addBean(thirdBean);

    final List<SomeBean> twoResultsTwoTuple = container.findByFieldValues(new FieldValueTuple<>(SomeBean.SOME_FIELD, 0),
        new FieldValueTuple<>(SomeBean.ANOTHER_FIELD, "anotherValue"));

    assertEquals(2, twoResultsTwoTuple.size());
    assertSame(firstBean, twoResultsTwoTuple.get(0));
    assertSame(thirdBean, twoResultsTwoTuple.get(1));
  }

  @Test
  public void testInheritance()
  {
    final IBeanContainer<SomeBean> baseTypeContainer = IBeanContainer.empty(SomeBean.class);

    baseTypeContainer.addBean(new SomeSpecialBean(34, 12));
    baseTypeContainer.addBean(new SomeBean(1));

    assertEquals(2, baseTypeContainer.size());
    assertSame(baseTypeContainer.getBean(0).getClass(), SomeSpecialBean.class);
  }

  /**
   * Some bean for the container.
   */
  public static class SomeBean extends OJBean implements Comparable<SomeBean>
  {
    @Identifier
    public static final IntegerField SOME_FIELD = OJFields.create(SomeBean.class);
    public static final TextField ANOTHER_FIELD = OJFields.create(SomeBean.class);

    public SomeBean()
    {
      this(0);
    }

    public SomeBean(int pValue)
    {
      setValue(SOME_FIELD, pValue);
      setValue(ANOTHER_FIELD, "anotherValue");
    }

    @Override
    public int compareTo(@NotNull SomeBean pBean)
    {
      return getValue(SOME_FIELD) - pBean.getValue(SOME_FIELD);
    }
  }

  /**
   * Some special bean extending {@link SomeBean}
   */
  public static class SomeSpecialBean extends SomeBean
  {
    public static final IntegerField SOME_SPECIAL_FIELD = OJFields.create(SomeSpecialBean.class);

    public SomeSpecialBean(int pValue, int pSpecialValue)
    {
      super(pValue);
      setValue(SOME_SPECIAL_FIELD, pSpecialValue);
    }
  }
}