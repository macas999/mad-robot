/*******************************************************************************
 * Copyright (c) 2012 MadRobot.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Lesser Public License v2.1
 *  which accompanies this distribution, and is available at
 *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *  
 *  Contributors:
 *  Elton Kent - initial API and implementation
 ******************************************************************************/
 
package com.madrobot.di.wizard.xml;

import com.madrobot.di.wizard.xml.converters.Converter;
import com.madrobot.di.wizard.xml.converters.SingleValueConverter;

public abstract class MapperWrapper implements Mapper {

    private final Mapper wrapped;

    public MapperWrapper(Mapper wrapped) {
        this.wrapped = wrapped;
    }

    public String serializedClass(Class type) {
        return wrapped.serializedClass(type);
    }

    public Class realClass(String elementName) {
        return wrapped.realClass(elementName);
    }

    public String serializedMember(Class type, String memberName) {
        return wrapped.serializedMember(type, memberName);
    }

    public String realMember(Class type, String serialized) {
        return wrapped.realMember(type, serialized);
    }

    public boolean isImmutableValueType(Class type) {
        return wrapped.isImmutableValueType(type);
    }

    public Class defaultImplementationOf(Class type) {
        return wrapped.defaultImplementationOf(type);
    }

    public String aliasForAttribute(String attribute) {
        return wrapped.aliasForAttribute(attribute);
    }

    public String attributeForAlias(String alias) {
        return wrapped.attributeForAlias(alias);
    }

    public String aliasForSystemAttribute(String attribute) {
        return wrapped.aliasForSystemAttribute(attribute);
    }

    public String getFieldNameForItemTypeAndName(Class definedIn, Class itemType, String itemFieldName) {
        return wrapped.getFieldNameForItemTypeAndName(definedIn, itemType, itemFieldName);
    }

    public Class getItemTypeForItemFieldName(Class definedIn, String itemFieldName) {
        return wrapped.getItemTypeForItemFieldName(definedIn, itemFieldName);
    }

    public ImplicitCollectionMapping getImplicitCollectionDefForFieldName(Class itemType, String fieldName) {
        return wrapped.getImplicitCollectionDefForFieldName(itemType, fieldName);
    }

    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
        return wrapped.shouldSerializeMember(definedIn, fieldName);
    }

    /**
     * @deprecated As of 1.3, use {@link #getConverterFromItemType(String, Class, Class)}
     */
    public SingleValueConverter getConverterFromItemType(String fieldName, Class type) {
        return wrapped.getConverterFromItemType(fieldName, type);
    }

    /**
     * @deprecated As of 1.3, use {@link #getConverterFromItemType(String, Class, Class)}
     */
    public SingleValueConverter getConverterFromItemType(Class type) {
        return wrapped.getConverterFromItemType(type);
    }

    /**
     * @deprecated As of 1.3, use {@link #getConverterFromAttribute(Class, String, Class)}
     */
    public SingleValueConverter getConverterFromAttribute(String name) {
        return wrapped.getConverterFromAttribute(name);
    }

    public Converter getLocalConverter(Class definedIn, String fieldName) {
        return wrapped.getLocalConverter(definedIn, fieldName);
    }

    public Mapper lookupMapperOfType(Class type) {
        return type.isAssignableFrom(getClass()) ? this : wrapped.lookupMapperOfType(type);
    }
    
    public SingleValueConverter getConverterFromItemType(String fieldName, Class type, Class definedIn) {
    	return wrapped.getConverterFromItemType(fieldName, type, definedIn);
    }
    
    /**
     * @deprecated As of 1.3, use combination of {@link #serializedMember(Class, String)} and {@link #getConverterFromItemType(String, Class, Class)} 
     */
    public String aliasForAttribute(Class definedIn, String fieldName) {
    	return wrapped.aliasForAttribute(definedIn, fieldName);
    }
    
    /**
     * @deprecated As of 1.3, use combination of {@link #realMember(Class, String)} and {@link #getConverterFromItemType(String, Class, Class)} 
     */
    public String attributeForAlias(Class definedIn, String alias) {
    	return wrapped.attributeForAlias(definedIn, alias);
    }
    
    /**
     * @deprecated As of 1.3.1, use {@link #getConverterFromAttribute(Class, String, Class)} 
     */
    public SingleValueConverter getConverterFromAttribute(Class type, String attribute) {
    	return wrapped.getConverterFromAttribute(type, attribute);
    }
    
    public SingleValueConverter getConverterFromAttribute(Class definedIn, String attribute, Class type) {
        return wrapped.getConverterFromAttribute(definedIn, attribute, type);
    }

}
