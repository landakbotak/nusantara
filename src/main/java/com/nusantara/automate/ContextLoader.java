package com.nusantara.automate;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.nusantara.automate.annotation.FetchSession;
import com.nusantara.automate.annotation.MapAction;
import com.nusantara.automate.annotation.MapActionList;
import com.nusantara.automate.annotation.MapField;
import com.nusantara.automate.annotation.MapSerializable;
import com.nusantara.automate.annotation.MapSession;
import com.nusantara.automate.annotation.MapType;
import com.nusantara.automate.util.ReflectionUtils;

/**
 * Manage context of object and it is used for direct injection
 * 
 * @author ari.patriana
 *
 */
public class ContextLoader {
	
	private static Logger log = LoggerFactory.getLogger(ContextLoader.class);
	
	private static WebExchange webExchange;
	
	public static void setWebExchange(WebExchange webExchange) {
		ContextLoader.webExchange = webExchange;
	}
	
	public static WebExchange getWebExchange() {
		return webExchange;
	}
	
	public static boolean isPersistentSerializable(Class<?> clazz) {
		return clazz.isAnnotationPresent(MapSerializable.class);
	}
	
	public static boolean isPersistentSerializable(Object object) {
		Class<?> clazz = object.getClass();
		return isPersistentSerializable(clazz);
	}
	
	public static boolean isLocalVariable(Class<?> clazz) {
		if (clazz.isAnnotationPresent(MapSerializable.class)) {
			MapType type = clazz.getAnnotation(MapSerializable.class).type();
			return type.equals(MapType.LOCAL);					
		}
		return false;
	}
	
	public static boolean isLocalVariable(Object object) {
		Class<?> clazz = object.getClass();
		return isLocalVariable(clazz);
	}
	
	public static boolean isCompositeVariable(Class<?> clazz) {
		if (clazz.isAnnotationPresent(MapSerializable.class)) {
			MapType type = clazz.getAnnotation(MapSerializable.class).type();
			return type.equals(MapType.COMPOSITE);					
		}
		return false;
	}
	
	public static boolean isCompositeVariable(Object object) {
		Class<?> clazz = object.getClass();
		return isCompositeVariable(clazz);
	}
	
	public static void setObject(Object object) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (getWebExchange() != null) {
			map.putAll(getWebExchange().getAll());
			map.put("all_local_variable", getWebExchange().getAllListLocalMap());
			//map.put("local_variable", getWebExchange().getLocalMap());		
			map.putAll(getWebExchange().getLocalMap());
		}
		setObject(object, map);
	}
	
	public static void setObjectLocal(Object object) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (getWebExchange() != null) {
			map.putAll(getWebExchange().getAll());
			map.put("all_local_variable", getWebExchange().getAllListLocalMap());
			map.put("local_variable", getWebExchange().getLocalMap());		
			map.putAll(getWebExchange().getLocalMap());
		}
		setObject(object, map);
	}
	
	public static void setObjectWithCustom(Object object, Map<String, Object> metadata) {
		Map<String, Object> map = new HashMap<String, Object>(metadata);
		if (getWebExchange() != null) {
			map.putAll(getWebExchange().getAll());
			map.put("all_local_variable", getWebExchange().getAllListLocalMap());
//			map.put("local_variable", getWebExchange().getLocalMap());	
			map.putAll(getWebExchange().getLocalMap());
		}
		setObject(object, map);
	}
	
	public static void setObjectLocalWithCustom(Object object, Map<String, Object> metadata) {
		Map<String, Object> map = new HashMap<String, Object>(metadata);
		if (getWebExchange() != null) {
			map.putAll(getWebExchange().getAll());
			map.put("all_local_variable", getWebExchange().getAllListLocalMap());
			map.put("local_variable", getWebExchange().getLocalMap());	
			map.putAll(getWebExchange().getLocalMap());
		}
		setObject(object, map);
	}

	
	private static void setObject(Object object, Map<String, Object> metadata) {
		Class<?> clazz = object.getClass();
		if (clazz.isAnnotationPresent(MapSerializable.class)) {		
			Map<String, String> fields = recognize(object, metadata);
			setFields(object, fields, metadata);
		} else {
			Map<String, String> fields = recognizeValue(object, metadata);
			setFields(object, fields, metadata);
		}
	}
	
	private static Map<String, String> recognizeValue(Object object, Map<String, Object> metadata) {
		return recognizeValue(object, object.getClass(), metadata);
	}
	
	private static Map<String, String> recognizeValue(Object object, Class<?> clazz, Map<String, Object> metadata) {
		Map<String, String> fields  = new HashMap<String, String>();    
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Value.class)) {
            	 fields.put(field.getName(), field.getAnnotation(Value.class).value());
            } else if (field.isAnnotationPresent(FetchSession.class)) {
            	try {
                	if (!checkAssignableFrom(field.getType(), List.class)) throw new InstantiationException("Exception for initialize field " + field.getName()  + " must be List");
                	ReflectionUtils.setProperty(object, field.getName(), metadata.get("all_local_variable"));
            	} catch (InstantiationException e) {
            		log.error("ERROR ", e);
            	}
            }
        }
        if (!Object.class.equals(clazz.getSuperclass())) {
        	Map<String, String> temp = recognizeValue(object, clazz.getSuperclass(), metadata);
        	if (temp != null && temp.size() > 0) {
        		fields.putAll(temp);
        	}
        }
        return fields;
	}

	private static Map<String, String> recognize(Object object, Map<String, Object> metadata) {
		return recognize(object, object.getClass(), metadata);
	}
	
	private static boolean checkAssignableFrom(Class<?> sourceClass, Class<?> targetClass) {
		if (sourceClass.isAssignableFrom(targetClass))
			return true;
		
		boolean result = false;
		for (Class<?> c : sourceClass.getInterfaces()) {
			if (c.isAssignableFrom(Class.class))
				return false;
			result = checkAssignableFrom(c, targetClass);
			if (result) break;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> recognize(Object object, Class<?> clazz, Map<String, Object> metadata) {
		Map<String, String> fields  = new HashMap<String, String>();    
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(MapField.class)) {
                fields.put(field.getName(), field.getAnnotation(MapField.class).name());
            } else if (field.isAnnotationPresent(MapAction.class)) {
				try {
					Class<?> c = field.getAnnotation(MapAction.class).clazz();
					Object d = c.newInstance();
					setObject(d, (Map<String, Object>)metadata.get(field.getAnnotation(MapAction.class).name()));
					ReflectionUtils.setProperty(object, field.getName(), d);
//					BeanUtils.setProperty(object, field.getName(), d);
				} catch (InstantiationException e) {
					log.error("ERROR ", e);
				} catch (IllegalAccessException e1) {
					log.error("ERROR ", e1);
				}
            } else if (field.isAnnotationPresent(MapActionList.class)) {
            	try {
	            	Class<?> c = field.getAnnotation(MapActionList.class).clazz();
	            	LinkedList<Object> list = new LinkedList<Object>();
	            	for (LinkedHashMap<String, Object> md : (Collection<LinkedHashMap<String, Object>>)metadata.get(field.getAnnotation(MapActionList.class).name())) {
	            		Object d = c.newInstance();
	            		setObjectWithCustom(d, md);
	            		list.add(d);
	            	};
	            	ReflectionUtils.setProperty(object, field.getName(), list);
//	            	BeanUtils.setProperty(object, field.getName(), list);
    			} catch (InstantiationException e) {
    				log.error("ERROR ", e);
				} catch (IllegalAccessException e1) {
					log.error("ERROR ", e1);
				}
            } else if (field.isAnnotationPresent(MapSession.class)) {
            	try {
                	if (!checkAssignableFrom(field.getType(), Map.class)) throw new InstantiationException("Exception for initialize field " + field.getName()  + " must be Map");
                	ReflectionUtils.setProperty(object, field.getName(), metadata.get("local_variable"));
            	} catch (InstantiationException e) {
            		log.error("ERROR ", e);
            	}
            } else if (field.isAnnotationPresent(Value.class)) {
            	 fields.put(field.getName(), field.getAnnotation(Value.class).value());
            } else if (field.isAnnotationPresent(FetchSession.class)) {
            	try {
                	if (!checkAssignableFrom(field.getType(), List.class)) throw new InstantiationException("Exception for initialize field " + field.getName()  + " must be List");
                	ReflectionUtils.setProperty(object, field.getName(), metadata.get("all_local_variable"));
            	} catch (InstantiationException e) {
            		log.error("ERROR ", e);
            	}
            }
            
        }
        
        if (!Object.class.equals(clazz.getSuperclass())) {
        	Map<String, String> temp = recognize(object, clazz.getSuperclass(), metadata);
        	if (temp != null && temp.size() > 0) {
        		fields.putAll(temp);
        	}
        }
        return fields;
	}
	
	private static void setFields(Object object, Map<String, String> fields, Map<String, Object> metadata) {
		for (Entry<String, String> entry : fields.entrySet()) {
			Object value = metadata.get(entry.getValue());
			if (value != null)
				ReflectionUtils.setProperty(object, entry.getKey(), String.valueOf(value));
		}        
	}
	
	public static void clear() {
		if (getWebExchange() != null) {
			getWebExchange().clear();
		}
	}
	

}
