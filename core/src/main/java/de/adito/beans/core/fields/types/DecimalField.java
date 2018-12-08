package de.adito.beans.core.fields.types;

import de.adito.beans.core.annotations.internal.TypeDefaultField;
import de.adito.beans.core.fields.serialization.*;
import de.adito.beans.core.util.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A bean field that holds a decimal number (double).
 *
 * @author Simon Danner, 23.08.2016
 */
@TypeDefaultField(types = Double.class)
public class DecimalField extends AbstractField<Double> implements ISerializableFieldToString<Double>
{
  public DecimalField(@NotNull String pName, @NotNull Collection<Annotation> pAnnotations)
  {
    super(Double.class, pName, pAnnotations);
  }

  @Override
  public Double getDefaultValue()
  {
    return 0.0;
  }

  @Override
  public Double getInitialValue()
  {
    return 0.0;
  }

  @Override
  public Double copyValue(Double pValue, ECopyMode pMode, CustomFieldCopy<?>... pCustomFieldCopies)
  {
    return pValue;
  }

  @Override
  public Double fromPersistent(String pSerialString)
  {
    return pSerialString == null ? getDefaultValue() : Double.parseDouble(pSerialString);
  }
}