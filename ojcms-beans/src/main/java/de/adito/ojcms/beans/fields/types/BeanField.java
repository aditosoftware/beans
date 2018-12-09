package de.adito.ojcms.beans.fields.types;

import de.adito.ojcms.beans.*;
import de.adito.ojcms.beans.annotations.internal.*;
import de.adito.ojcms.beans.util.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * A bean field that holds a bean.
 *
 * @param <BEAN> the generic type of the bean that this field is referring to
 * @author Simon Danner, 09.02.2017
 */
@TypeDefaultField(types = IBean.class)
@ReferenceField(resolverType = EReferableResolver.SINGLE)
public class BeanField<BEAN extends IBean<BEAN>> extends AbstractField<BEAN>
{
  public BeanField(@NotNull Class<BEAN> pType, @NotNull String pName, @NotNull Collection<Annotation> pAnnotations)
  {
    super(pType, pName, pAnnotations);
  }

  @Override
  public BEAN copyValue(BEAN pValue, ECopyMode pMode, CustomFieldCopy<?>... pCustomFieldCopies)
  {
    return pValue.createCopy(pMode, pCustomFieldCopies);
  }
}