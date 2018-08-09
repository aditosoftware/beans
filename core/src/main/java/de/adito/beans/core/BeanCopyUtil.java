package de.adito.beans.core;

import de.adito.beans.core.util.*;
import de.adito.beans.core.util.beancopy.*;
import de.adito.beans.core.util.exceptions.*;
import org.objenesis.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Utility class for copying beans.
 *
 * @author Simon Danner, 12.04.2018
 */
final class BeanCopyUtil
{
  private static final Objenesis copyCreator = new ObjenesisStd();

  /**
   * Creates a copy of a bean.
   * If you want to use a custom constructor call to create the copy,
   * you may use {@link #createCopy(IBean, ECopyMode, Function, CustomFieldCopy[])}.
   *
   * @param pOriginal     the original bean to create the copy of
   * @param pMode         the copy mode
   * @param pCustomCopies a collection of custom copy mechanisms for specific bean fields
   * @return a copy of the bean
   */
  static <BEAN extends IBean<BEAN>> BEAN createCopy(BEAN pOriginal, ECopyMode pMode, CustomFieldCopy<?>... pCustomCopies)
  {
    BeanUtil.requiresDeclaredBeanType(pOriginal.getClass());
    //noinspection unchecked
    BEAN copyInstance = (BEAN) copyCreator.newInstance(pOriginal.getClass());
    EncapsulatedBuilder.injectCustomEncapsulated(copyInstance,
                                                 new Bean.DefaultEncapsulatedBuilder(pOriginal.streamFields().collect(Collectors.toList())));
    return _setValues(pOriginal, copyInstance, pMode, pCustomCopies);
  }

  /**
   * Creates a copy of a bean.
   * This method should be used, if you want to use a custom constructor call to create the copy.
   * Otherwise use {@link #createCopy(IBean, ECopyMode, CustomFieldCopy[])},
   * where you are not supposed to define a custom constructor call.
   *
   * @param pOriginal              the original bean to create the copy of
   * @param pMode                  the copy mode
   * @param pCustomConstructorCall a custom constructor call defined as function (the input is the existing bean, the function should create the copy)
   * @param pCustomCopies          a collection of custom copy mechanisms for specific bean fields
   * @return a copy of the bean
   */
  static <BEAN extends IBean<BEAN>> BEAN createCopy(BEAN pOriginal, ECopyMode pMode, Function<BEAN, BEAN> pCustomConstructorCall,
                                                    CustomFieldCopy<?>... pCustomCopies)
  {
    return _setValues(pOriginal, pCustomConstructorCall.apply(pOriginal), pMode, pCustomCopies);
  }

  /**
   * Sets the values of the original bean in the copied bean instance.
   * If a deep copy is requested, all deep values will be copied.
   *
   * @param pOriginal     the original bean
   * @param pCopy         the copied bean
   * @param pMode         the copy mode
   * @param pCustomCopies a collection of custom copy mechanisms for specific bean fields
   * @param <BEAN>        the type of the bean to set the values
   * @return the copy of the bean
   */
  private static <BEAN extends IBean<BEAN>> BEAN _setValues(BEAN pOriginal, BEAN pCopy, ECopyMode pMode, CustomFieldCopy<?>[] pCustomCopies)
  {
    Map<IField<?>, Function> customCopiesMap = ECopyMode.isDeep(pMode) ? _createCustomCopiesMap(pCustomCopies) : Collections.emptyMap();
    //noinspection unchecked,RedundantCast
    pCopy.streamFields()
        .forEach(pField -> pCopy.setValue((IField) pField, !ECopyMode.isDeep(pMode) ? pOriginal.getValue(pField) :
            _copyFieldValue((IField) pField, pOriginal.getValue(pField), pMode,
                            Optional.ofNullable(customCopiesMap.get(pField)), pCustomCopies)));
    //If required, set non bean values as well
    if (ECopyMode.allFields(pMode))
      BeanReflector.reflectDeclaredNonBeanFields(pOriginal.getClass()).stream()
          .peek(pField -> {
            if (!pField.isAccessible())
              pField.setAccessible(true);
          })
          .forEach(pField -> {
            try
            {
              pField.set(pCopy, pField.get(pOriginal));
            }
            catch (IllegalAccessException pE)
            {
              throw new RuntimeException("Unable to set non bean value while copying a bean!", pE);
            }
          });
    return pCopy;
  }

  /**
   * Creates a copy of a certain field value.
   *
   * @param pField               the bean field to copy the value of
   * @param pValue               the value to copy
   * @param pMode                the copy mode
   * @param pOptionalCopyCreator a optional function to create the copy {@link CustomFieldCopy}
   * @param pCustomCopies        a collection of custom copy mechanisms for specific bean fields
   * @param <TYPE>               the generic type of the field value
   * @return the copied value
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static <TYPE> TYPE _copyFieldValue(IField<TYPE> pField, TYPE pValue, ECopyMode pMode,
                                             Optional<Function<TYPE, TYPE>> pOptionalCopyCreator, CustomFieldCopy<?>[] pCustomCopies)
  {
    try
    {
      return pOptionalCopyCreator
          .map(pCreator -> pCreator.apply(pValue))
          .orElse(pField.copyValue(pValue, pMode, pCustomCopies));
    }
    catch (BeanCopyUnsupportedException pE)
    {
      throw new BeanCopyException(pE);
    }
  }

  /**
   * Creates a map from the array of {@link CustomFieldCopy}.
   *
   * @param pCustomCopies the custom field copy mechanisms
   * @return the map based on the array
   */
  private static Map<IField<?>, Function> _createCustomCopiesMap(CustomFieldCopy[] pCustomCopies)
  {
    return Stream.of(pCustomCopies)
        .collect(Collectors.toMap(CustomFieldCopy::getField, CustomFieldCopy::getCopyCreator));
  }
}
