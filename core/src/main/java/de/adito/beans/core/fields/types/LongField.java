package de.adito.beans.core.fields.types;

import de.adito.beans.core.annotations.internal.TypeDefaultField;
import de.adito.beans.core.fields.serialization.*;
import de.adito.beans.core.util.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A bean field that holds a long.
 *
 * @author Simon Danner, 14.02.2017
 */
@TypeDefaultField(types = Long.class)
public class LongField extends AbstractField<Long> implements ISerializableFieldToString<Long>
{
  public LongField(@NotNull String pName, @NotNull Collection<Annotation> pAnnotations)
  {
    super(Long.class, pName, pAnnotations);
  }

  @Override
  public Long getDefaultValue()
  {
    return 0L;
  }

  @Override
  public Long getInitialValue()
  {
    return 0L;
  }

  @Override
  public Long copyValue(Long pValue, ECopyMode pMode, CustomFieldCopy<?>... pCustomFieldCopies)
  {
    return pValue;
  }

  @Override
  public Long fromPersistent(String pSerialString)
  {
    return pSerialString == null ? getDefaultValue() : Long.parseLong(pSerialString);
  }
}