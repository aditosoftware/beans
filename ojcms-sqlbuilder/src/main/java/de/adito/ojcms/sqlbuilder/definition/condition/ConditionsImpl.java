package de.adito.ojcms.sqlbuilder.definition.condition;

import de.adito.ojcms.sqlbuilder.format.IPreparedStatementFormat;

/**
 * Implementation for multiple, concatenated where conditions.
 * This class holds the added conditions and the concatenation types.
 *
 * @author Simon Danner, 09.06.2018
 */
class ConditionsImpl extends AbstractStatementConcatenation<IWhereConditions, ConditionsImpl> implements IWhereConditions
{
  /**
   * Creates a multiple where condition.
   *
   * @param pCondition the initial condition to start from
   */
  ConditionsImpl(IPreparedStatementFormat pCondition)
  {
    super(pCondition);
  }

  @Override
  public <VALUE> IWhereConditions and(IWhereCondition<VALUE> pCondition)
  {
    return addConcatenation(pCondition, EConcatenationType.AND);
  }

  @Override
  public IWhereConditions and(IWhereConditions pMultipleConditions)
  {
    return addConcatenation(pMultipleConditions, EConcatenationType.AND);
  }

  @Override
  public <VALUE> IWhereConditions or(IWhereCondition<VALUE> pCondition)
  {
    return addConcatenation(pCondition, EConcatenationType.OR);
  }

  @Override
  public IWhereConditions or(IWhereConditions pMultipleConditions)
  {
    return addConcatenation(pMultipleConditions, EConcatenationType.OR);
  }
}
