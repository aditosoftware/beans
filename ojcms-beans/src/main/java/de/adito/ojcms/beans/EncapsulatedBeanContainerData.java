package de.adito.ojcms.beans;

import de.adito.ojcms.beans.annotations.Statistics;
import de.adito.ojcms.beans.annotations.internal.EncapsulatedData;
import de.adito.ojcms.beans.datasource.IBeanContainerDataSource;
import de.adito.ojcms.beans.reactive.IEvent;
import de.adito.ojcms.beans.statistics.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.*;

/**
 * The encapsulated bean container data implementation based on a data source.
 *
 * @param <BEAN> the type of the beans in data core.
 * @author Simon Danner, 08.12.2018
 */
@EncapsulatedData
class EncapsulatedBeanContainerData<BEAN extends IBean<BEAN>> extends AbstractEncapsulatedData<BEAN, IBeanContainerDataSource<BEAN>>
    implements IEncapsulatedBeanContainerData<BEAN>
{
  private final Class<BEAN> beanType;
  private _LimitInfo limitInfo = null;
  private final IStatisticData<Integer> statisticData;
  private final Map<BEAN, Disposable> beanDisposableMapping = new ConcurrentHashMap<>();

  /**
   * Creates the encapsulated bean container data core.
   *
   * @param pDataSource the data source for the data core
   * @param pBeanType   the type of the beans in this data core
   */
  EncapsulatedBeanContainerData(IBeanContainerDataSource<BEAN> pDataSource, Class<BEAN> pBeanType)
  {
    super(pDataSource);
    beanType = pBeanType;
    statisticData = _createStatisticData();
    StreamSupport.stream(pDataSource.spliterator(), false).forEach(this::_observeBean);
  }

  @Override
  public Class<BEAN> getBeanType()
  {
    return beanType;
  }

  @Override
  public void addBean(BEAN pBean, int pIndex)
  {
    if (pIndex < 0 || pIndex > size())
      throw new IndexOutOfBoundsException("index: " + pIndex);

    //Is the limit reached?
    if (limitInfo != null && limitInfo.limit == size())
    {
      if (!limitInfo.evicting)
        throw new RuntimeException("The limit of this container is reached! limit: " + limitInfo.limit);
      removeBean(getBean(0)); //Remove first bean if limit is reached and evicting flag is set
      pIndex--;
    }

    getDatasource().addBean(_observeBean(pBean), pIndex);
  }

  @Override
  public BEAN replaceBean(BEAN pReplacement, int pIndex)
  {
    if (pIndex < 0 || pIndex >= size())
      throw new IndexOutOfBoundsException("index: " + pIndex);

    BEAN removed = removeBean(pIndex);
    addBean(pReplacement, pIndex);
    return removed;
  }

  @Override
  public boolean removeBean(BEAN pBean)
  {
    beanDisposableMapping.remove(pBean).dispose();
    return getDatasource().removeBean(pBean);
  }

  @Override
  public BEAN removeBean(int pIndex)
  {
    if (pIndex < 0 || pIndex >= size())
      throw new IndexOutOfBoundsException("index: " + pIndex);

    return getDatasource().removeBean(pIndex);
  }

  @Override
  public BEAN getBean(int pIndex)
  {
    return getDatasource().getBean(pIndex);
  }

  @Override
  public int indexOfBean(BEAN pBean)
  {
    return getDatasource().indexOfBean(pBean);
  }

  @Override
  public int size()
  {
    return getDatasource().size();
  }

  @Override
  public void sort(Comparator<BEAN> pComparator)
  {
    getDatasource().sort(pComparator);
  }

  @Override
  public void setLimit(int pMaxCount, boolean pEvicting)
  {
    if (pMaxCount >= 0)
    {
      int diffToMany = size() - pMaxCount;
      if (diffToMany > 0)
        IntStream.range(0, diffToMany)
            .forEach(pIndex -> removeBean(0));
    }
    limitInfo = pMaxCount < 0 ? null : new _LimitInfo(pMaxCount, pEvicting);
  }

  @Nullable
  @Override
  public IStatisticData<Integer> getStatisticData()
  {
    return statisticData;
  }

  @NotNull
  @Override
  public Iterator<BEAN> iterator()
  {
    return getDatasource().iterator();
  }

  /**
   * Creates the statistic data for this encapsulated core.
   * This data is an amount of timestamps with an associated number,
   * which stands for the amount of beans in this container at the timestamp.
   *
   * @return the statistic data for this encapsulated core
   */
  @Nullable
  private IStatisticData<Integer> _createStatisticData()
  {
    Statistics statistics = beanType.getAnnotation(Statistics.class);
    return statistics != null ? new StatisticData<>(statistics.capacity(), size()) : null;
  }

  /**
   * Observers value and field changes of a bean within this container.
   *
   * @param pBean the bean to observe
   * @return the observed bean
   */
  private BEAN _observeBean(BEAN pBean)
  {
    final Observable<IEvent<BEAN>> combinedObservables = Observable.concat(pBean.observeValues(), pBean.observeFieldAdditions(),
                                                                           pBean.observeFieldRemovals());
    //noinspection unchecked
    final Disposable disposable = combinedObservables
        .subscribe(pChangeEvent -> getEventObserverFromType((Class<IEvent<BEAN>>) pChangeEvent.getClass()).onNext(pChangeEvent));
    beanDisposableMapping.put(pBean, disposable);
    return pBean;
  }

  /**
   * Information about the limit of this container core.
   * Contains the limit itself and the information if old entries should be evicted.
   */
  private class _LimitInfo
  {
    private final int limit;
    private final boolean evicting;

    public _LimitInfo(int pLimit, boolean pEvicting)
    {
      limit = pLimit;
      evicting = pEvicting;
    }
  }
}