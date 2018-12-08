package de.adito.beans.core.fields.types;

import de.adito.beans.core.annotations.internal.TypeDefaultField;
import de.adito.beans.core.fields.serialization.ISerializableFieldToString;
import de.adito.beans.core.util.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A bean field that holds an Integer.
 *
 * @author Simon Danner, 27.01.2017
 */
@TypeDefaultField(types = Integer.class)
public class IntegerField extends AbstractField<Integer> implements ISerializableFieldToString<Integer>
{
  public IntegerField(@NotNull String pName, @NotNull Collection<Annotation> pAnnotations)
  {
    super(Integer.class, pName, pAnnotations);
  }

  @Override
  public Integer getDefaultValue()
  {
    return 0;
  }

  @Override
  public Integer getInitialValue()
  {
    return 0;
  }

  @Override
  public Integer copyValue(Integer pValue, ECopyMode pMode, CustomFieldCopy<?>... pCustomFieldCopies)
  {
    return pValue;
  }

  @Override
  public Integer fromPersistent(String pSerialString)
  {
    return pSerialString == null ? getDefaultValue() : Integer.parseInt(pSerialString);
  }
}