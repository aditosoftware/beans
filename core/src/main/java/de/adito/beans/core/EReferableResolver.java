package de.adito.beans.core;

import de.adito.beans.core.annotations.internal.RequiresEncapsulatedAccess;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Variants to resolve {@link IReferable} instances from a bean value.
 *
 * @author Simon Danner, 24.11.2018
 */
@RequiresEncapsulatedAccess
public enum EReferableResolver
{
  /**
   * Resolver for single referable values. (example: single bean)
   */
  SINGLE(EReferableResolver::_single),

  /**
   * Resolver for multi referable values. (example: bean container)
   */
  MULTI(EReferableResolver::_withMulti);

  private final Function<Object, Stream<IReferable>> resolver;

  /**
   * A resolver type to get referables from a field's value.
   *
   * @param pResolver the resolver function
   */
  EReferableResolver(Function<Object, Stream<IReferable>> pResolver)
  {
    resolver = pResolver;
  }

  /**
   * The resolver function to transform a field's value into a stream of referables.
   *
   * @return the resolver function
   */
  Function<Object, Stream<IReferable>> getResolver()
  {
    return resolver;
  }

  private static Stream<IReferable> _single(Object pValue)
  {
    return _tryGetEncapsulated(pValue)
        .map(pEncapsulated -> Stream.of((IReferable) pEncapsulated))
        .orElse(Stream.empty());
  }

  /**
   * Takes the encapsulated data core from a bean field's value as {@link IReferable}
   * and tries to add more referables to a stream from the data core's elements, if they contain encapsulated cores as well.
   *
   * @param pValue the value of the field
   * @return a stream of referables retrieved from the field's value
   */
  private static Stream<IReferable> _withMulti(Object pValue)
  {
    return _tryGetEncapsulated(pValue)
        .map(pEncapsulated -> Stream.concat(pEncapsulated.stream()
                                                .filter(pElement -> pElement instanceof IEncapsulatedHolder)
                                                .map(EReferableResolver::_toEncapsulated)
                                                .map(pInnerEncapsulated -> (IReferable) pInnerEncapsulated),
                                            Stream.of(pEncapsulated)))
        .orElse(Stream.empty());
  }

  /**
   * Retrieves an encapsulated data core from a bean field's value that is an {@link IEncapsulatedHolder}.
   *
   * @param pValue the value of the field
   * @return the retrieved encapsulated data core
   * @throws RuntimeException if the value is no {@link IEncapsulatedHolder}
   */
  private static Optional<IEncapsulated<?>> _tryGetEncapsulated(Object pValue)
  {
    return Optional.ofNullable(pValue)
        .map(pHolder -> _toEncapsulated(pValue));
  }

  private static IEncapsulated<?> _toEncapsulated(Object pValue)
  {
    try
    {
      return ((IEncapsulatedHolder<?>) pValue).getEncapsulated();
    }
    catch (ClassCastException pE)
    {
      throw new RuntimeException("The field's value should hold an encapsulated! type " + pValue.getClass().getName());
    }
  }
}