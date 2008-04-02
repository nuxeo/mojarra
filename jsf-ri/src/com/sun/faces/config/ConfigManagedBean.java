/*
 * $Id: ConfigManagedBean.java,v 1.8 2003/09/15 16:29:20 rkitain Exp $
 */

/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.faces.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.faces.util.Util;

import javax.faces.FacesException;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.IntrospectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Config Bean for a Managed Bean .</p>
 */
public class ConfigManagedBean extends ConfigFeature implements Cloneable {

    //
    // Protected Constants
    //

    // Log instance for this class
    protected static Log log = LogFactory.getLog(ConfigManagedBean.class);
    
    private String managedBeanId;
    private String managedBeanClass;
    private String managedBeanScope;
    private String managedBeanCreate;

    private HashMap properties = null;

    public String getManagedBeanId() {
        return (this.managedBeanId);
    }
    public void setManagedBeanId(String managedBeanId) {
        this.managedBeanId = managedBeanId;
    }

    public String getManagedBeanClass() {
        return (this.managedBeanClass);
    }
    public void setManagedBeanClass(String managedBeanClass) {
        this.managedBeanClass = managedBeanClass;
    }

    public String getManagedBeanScope() {
        return (this.managedBeanScope);
    }
    public void setManagedBeanScope(String managedBeanScope) {
        this.managedBeanScope = managedBeanScope;
    }

    public String getManagedBeanCreate() {
        return (this.managedBeanCreate);
    }
    public void setManagedBeanCreate(String managedBeanCreate) {
        this.managedBeanCreate = managedBeanCreate;
    }
    
    public void addProperty(ConfigManagedBeanProperty property) throws FacesException {
        if (null == property) {
            throw new NullPointerException
                (Util.getExceptionMessage(Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }
        if (properties == null) {
            properties = new HashMap();
        }
        Class propertyType = getPropertyType(property);
        if (propertyType != null) {
            property.convertValue(replaceIfPrimitive(propertyType));
        }
        properties.put(property.getPropertyName(), property);
    }
    public Map getProperties() {
        if (properties == null) {
            return (Collections.EMPTY_MAP);
        } else {
            return (properties);
        }
    }

    public Object clone() {
        ConfigManagedBean cmb = null;
        try {
            cmb = (ConfigManagedBean)super.clone();
            if (properties != null) {
                cmb.properties = (HashMap)properties.clone();
            }
        } catch (CloneNotSupportedException e) {
        }
        return cmb;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ID:"+getManagedBeanId()+
            "\nCLASS:"+getManagedBeanClass()+
            "\nSCOPE:"+getManagedBeanScope()+
            "\nCREATE:"+getManagedBeanCreate()+
            "\nPROPERTIES...");
        if (properties.size() > 0) {
            Iterator iter = properties.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                ConfigManagedBeanProperty cmbp = (ConfigManagedBeanProperty)properties.get(key);
                String name = cmbp.getPropertyName();
                sb.append("\n    NAME:"+cmbp.getPropertyName());
                if (cmbp.hasValuesArray()) {
                    sb.append("\n    VALUES:");
                    List values = cmbp.getValues();
                    for (int i = 0, size = values.size(); i < size; i++) {
                        ConfigManagedBeanPropertyValue cmbpv = 
                            (ConfigManagedBeanPropertyValue)values.get(i);
                        sb.append("\n      VALUE:CATEGORY:"+cmbpv.getValueCategory()+
                            " : VALUE:"+cmbpv.getValue());
                    }
                } else if (cmbp.hasMapEntries()) {
                    sb.append("\n    MAP KEY CLASS:"+cmbp.getMapKeyClass());
                    sb.append("\n    MAP VALUE CLASS:"+cmbp.getMapValueClass());
                    sb.append("\n    MAP ENTRIES:");
                    List mapEntries = cmbp.getMapEntries();
                    for (int i = 0, size = mapEntries.size(); i < size; i++) {
                        ConfigManagedPropertyMap cmpm = 
                            (ConfigManagedPropertyMap)mapEntries.get(i);
                        sb.append("\n      KEY:"+cmpm.getKey()+
                            " : VALUE:CATEGORY:"+cmpm.getValueCategory()+
                            " : VALUE:"+cmpm.getValue());
                    }
                } else {
                    ConfigManagedBeanPropertyValue cmbpv = cmbp.getValue();
                    sb.append("\n    VALUE:CATEGORY:"+cmbpv.getValueCategory()+
                        " : VALUE:"+cmbpv.getValue());
                }
            }
        }
        return sb.toString();
    }

    private Class getPropertyType(ConfigManagedBeanProperty property) {
        boolean isUIComponent = false;
        
        Class propertyType = null;
        Class clazz = null;
        // indexed and mapped properties have explicit types
        if (!property.hasValuesArray() && !property.hasMapEntries()) {
            PropertyDescriptor descs[] = null;
            try {
                clazz = Util.loadClass
                    (managedBeanClass, this);
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                descs = beanInfo.getPropertyDescriptors();
            } catch (ClassNotFoundException ex) {
                Object[] obj = new Object[1];
                obj[0] = managedBeanClass;
		String msg = Util.getExceptionMessage(Util.CANT_INSTANTIATE_CLASS_ERROR_MESSAGE_ID,
		    obj);
                if (log.isWarnEnabled()) {
                    log.warn(msg, ex);
                }
                throw new FacesException(msg, ex);
            } catch (IntrospectionException ex) {
                // if the property happens to be attribute on UIComponent
                // then bean introspection will fail and we need to return null.
                if (isUIComponentClass(clazz)) {
                    return null;
                }
                Object[] obj = new Object[1];
                obj[0] = managedBeanClass;
		String msg = Util.getExceptionMessage(Util.CANT_INTROSPECT_CLASS_ERROR_MESSAGE_ID,
		    obj);
                if (log.isWarnEnabled()) {
                    log.warn(msg, ex);
                }
                throw new FacesException(msg, ex);
            }
            PropertyDescriptor desc = null;

            for (int i = 0; i < descs.length; i++) {
                if (property.getPropertyName().equals(descs[i].getName())) {
                   desc = descs[i];
                   break;
               }
            }
            if (desc == null) {
                // if the property happens to be attribute on UIComponent
                // then bean introspection will fail and we need to return null.
                if (isUIComponentClass(clazz)) {
                    return null;
                }
                Object[] obj = new Object[1];
                obj[0] = managedBeanClass;
		String msg = Util.getExceptionMessage(Util.CANT_INTROSPECT_CLASS_ERROR_MESSAGE_ID,
		    obj);
                if (log.isWarnEnabled()) {
                    log.warn(msg);
                }
                throw new FacesException(msg);
            }

            boolean isIndexed;
            if (desc instanceof IndexedPropertyDescriptor) {
               isIndexed = true;
               propertyType = 
                   ((IndexedPropertyDescriptor) desc).getIndexedPropertyType();
            } else {
               isIndexed = false;
               propertyType = desc.getPropertyType();
            }
        }

        return propertyType;
    }
    
    /**
     * Determines if the class or interface represented by clazz object is 
     * either the same as, or is a superclass or superinterface of, 
     * <code>javax.faces.component.UIComponent</code>
     */
    public boolean isUIComponentClass(Class clazz) {
        Class uiComponentClass = null;
        try {
            uiComponentClass = 
                Util.loadClass("javax.faces.component.UIComponent", this);
        } catch (ClassNotFoundException cfe) {
            Object[] obj = new Object[1];
            obj[0] = uiComponentClass;
	    String msg = Util.getExceptionMessage(Util.CANT_INSTANTIATE_CLASS_ERROR_MESSAGE_ID,
	        obj);
            if (log.isWarnEnabled()) {
                log.warn(msg, cfe);
            }
            throw new FacesException(msg, cfe);
        }
        if (uiComponentClass.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    /**
     * check to see if value is a primitive and convert it to the
     * corresponding class.
     *
     * @param value the class object returned from the Bean's PropertyDescriptor
     *
     * @return true if value is a primitive type
     */
    private static Class replaceIfPrimitive(Class value) {
        if (value.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (value.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (value.equals(Character.TYPE)) {
            return Character.class;
        } else if (value.equals(Double.TYPE)) {
            return Double.class;
        } else if (value.equals(Float.TYPE)) {
            return Float.class;
        } else if (value.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (value.equals(Long.TYPE)) {
            return Long.class;
        } else if (value.equals(Short.TYPE)) {
            return Short.class;
        }
        return value;
    }


}
