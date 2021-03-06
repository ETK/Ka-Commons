package org.kasource.commons.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kasource.commons.reflection.filter.MethodFilterBuilder;
import org.kasource.commons.reflection.filter.classes.ClassFilter;
import org.kasource.commons.reflection.filter.constructors.ConstructorFilter;
import org.kasource.commons.reflection.filter.fields.FieldFilter;
import org.kasource.commons.reflection.filter.methods.MethodFilter;
import org.kasource.commons.reflection.filter.methods.MethodFilterList;
import org.kasource.commons.util.reflection.MethodUtils;

/**
 * Class Introspection.
 * 
 * This class can be used to extract information about class meta data
 * such as interfaces, methods and fields.
 * <p>
 * The difference between getFields and getDeclaredField is that getFields
 * returns fields declared by super classes as well as fields declared by
 * the target class. While getDeclaredField only returns fields declared by 
 * the target class.
 * <p>
 * The same pattern is applied for any getXXX and getDeclaredXXX method pairs.
 * <p>
 * ClassFilter, FieldFilter and MethodFilter is used to query (filter) for interfaces, fields 
 * and methods.
 * <p>
 * Example: Finding all getters:
 * {@code
 * ClassIntroSpector introspector = new ClassIntroSpector(MyClass.class);
 * Set<Method> getters = introspector.getMethods(new MethodFilterBuilder().name("get[A-Z].*").or().name("is[A-Z].*").or().name("has[A-Z]*").isPublic().not().returnType(Void.TYPE).numberOfParameters(0).build());
 * }
 * 
 * @author rikardwi
 **/
public class ClassIntrospector {

    private Class<?> target;
    
    /**
     * Constructor.
     * 
     * @param target The target class to introspect.s
     **/
    public ClassIntrospector(Class<?> target) {
        this.target = target;
    }
    
    
    /**
     * Returns the named method from class <i>clazz</i>, does not throw checked exceptions.
     * 
     * @param clazz
     *            The class to inspect
     * @param name
     *            The name of the method to get
     * @param params
     *            Parameter types for the method
     * 
     * 
     * @return Returns the named method from class <i>clazz</i>.
     * 
     * @throws IllegalArgumentException if method could not be found or security 
     * issues occurred when trying to retrieve the method.
     */
    public Method getDeclaredMethod(String name, Class<?>... params) {
        try {
            return target.getDeclaredMethod(name, params);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not access method: " + name + " on " + target, e);
        }
    }
    
    /**
     * Returns the named public method from class <i>clazz</i> or any of its super classes, does not throw checked exceptions.
     * 
     * @param clazz
     *            The class to inspect
     * @param name
     *            The name of the method to get
     * @param params
     *            Parameter types for the method
     * 
     * 
     * @return Returns the named method from class <i>clazz</i>.
     * 
     * @throws IllegalArgumentException if method could not be found or security 
     * issues occurred when trying to retrieve the method.
     */
    public Method getMethod(String name, Class<?>... params) {
        try {
            return target.getMethod(name, params);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not access method: " + name + " on " + target, e);
        }
    }

    /**
     * Returns the methods declared by clazz which matches the supplied
     * method filter.
     * 
     * @param clazz
     *            The class to inspect
     * @param methodFilter
     *            The method filter to apply.
     * 
     * @return methods that match the params argument in their method signature
     * 
     **/
    public  Set<Method> getDeclaredMethods(MethodFilter methodFilter) {
        return getDeclaredMethods(target, methodFilter);
    }
    
    /**
     * Returns true if the supplied annotation is present on the target class
     * or any of its super classes.
     * 
     * @param annotation Annotation to find.
     * 
     * @return true if the supplied annotation is present on the target class
     * or any of its super classes.
     **/
    public  boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        Class<?> clazz = target;
        if (clazz.isAnnotationPresent(annotation)) {
            return true;
        }
        while((clazz = clazz.getSuperclass()) != null) {
            if (clazz.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the annotation of the annotationClass of the clazz or any of it super classes.
     * 
     * @param clazz
     *           The class to inspect.
     * @param annotationClass
     *           Class of the annotation to return
     *           
     * @return The annotation of annnotationClass if found else null.
     */
    public  <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        Class<?> clazz = target;
        T annotation = clazz.getAnnotation(annotationClass);
        if(annotation != null) {
            return annotation;
        }
        while((clazz = clazz.getSuperclass()) != null) {
            annotation = clazz.getAnnotation(annotationClass);
            if(annotation != null) {
                return annotation;
            }
        }
        return null;
    }
    
    /**
     * Returns the methods declared by clazz which matches the supplied
     * method filter.
     * 
     * @param clazz
     *            The class to inspect
     * @param methodFilter
     *            The method filter to apply.
     * 
     * @return methods that match the params argument in their method signature
     * 
     **/
    private Set<Method> getDeclaredMethods(Class<?> clazz, MethodFilter methodFilter) {
        Method[] methods = target.getDeclaredMethods();
        Set<Method> matches = new HashSet<Method>();
        for (Method method : methods) {       
            if (methodFilter.passFilter(method)) {
                matches.add(method);
            }
        }
        return matches;
    }

    /**
     * Returns the methods declared by the target class and any of its super classes, which matches the supplied
     * methodFilter.
     * 
     * @param methodFilter
     *            The method filter to apply.
     * 
     * @return methods that match the methodFilter.
     * 
     **/
    public  Set<Method> getMethods(MethodFilter methodFilter) {
        Class<?> clazz = target;
        Set<Method> matches = getDeclaredMethods(clazz, methodFilter);      
        while((clazz = clazz.getSuperclass()) != null) {     
            matches.addAll(getDeclaredMethods(clazz, methodFilter));
        }
        return matches;
    }
    
    /**
     * Returns the method declared by the target class and any of its super classes, which matches the supplied
     * methodFilter, if method is found null is returned. If more than one method is found the
     * first in the resulting set iterator is returned.
     * 
     * @param methodFilter
     *            The method filter to apply.
     * 
     * @return method that match the methodFilter or null if no such method was found.
     * 
     **/
    public  Method getMethod(MethodFilter methodFilter) {
        Set<Method> methods = getMethods(methodFilter);
        if (!methods.isEmpty()) {
            return methods.iterator().next();
        }
        return null;
    }
      
    
    /**
     * Returns a set of interfaces from the target class that passes the supplied filter.
     * This method also inspects any interfaces implemented by super classes.
     * 
     * @param filter            The class filter to use.
     * 
     * @return a set of interfaces from the target class that passes the supplied filter.
     */
    public  Set<Class<?>> getInterfaces(ClassFilter filter) {
        Class<?> clazz = target;
        Set<Class<?>> interfacesFound = getDeclaredInterfaces(clazz, filter);
        
        while((clazz = clazz.getSuperclass()) != null) {
            interfacesFound.addAll(getDeclaredInterfaces(clazz, filter));
        }
        return interfacesFound;
       
    }
    
    /**
     * Returns the interface from target class that passes the supplied filter.
     * This method also inspects any interfaces implemented by super classes.
     * 
     * If no interface is found null is returned.
     * 
     * @param filter  The class filter to use.
     * 
     * @return the interface from target class that passes the supplied filter, may 
     * be null if no match is found.
     */
    public  Class<?> getInterface(ClassFilter filter) {
        Set<Class<?>> interfaces = getInterfaces(filter);
        if (!interfaces.isEmpty()) {
            return interfaces.iterator().next();
        }
        return null;
    }

    
    /**
     * Returns a set of interfaces that that passes the supplied filter.
     * 
     * @param filter            The class filter to use.
     * 
     * @return all Interface classes from clazz that passes the filter.
     */
    public Set<Class<?>> getDeclaredInterfaces(ClassFilter filter) {
       return getDeclaredInterfaces(target, filter);
    }
    
    
    /**
     * Returns a set of interfaces that the from clazz that passes the supplied filter.
     * 
     * @param clazz             The class to inspect
     * @param filter            The class filter to use.
     * 
     * @return all Interface classes from clazz that passes the filter.
     */
    private Set<Class<?>> getDeclaredInterfaces(Class<?> clazz, ClassFilter filter) {
        Set<Class<?>> interfacesFound = new HashSet<Class<?>>();

        Class<?>[] interfaces = clazz.getInterfaces();
        for(Class<?> interfaceClass : interfaces) {
            if(filter.passFilter(interfaceClass)) {
                interfacesFound.add(interfaceClass);
            }
        }
        return interfacesFound;
    }
    
    /**
     * Returns a set of all fields matching the supplied filter
     * declared in the target class.
     * 
     * @param filter    Filter to use.
     * 
     * @return All matching fields declared by the target class.
     **/
    public  Set<Field> getDeclaredFields(FieldFilter filter) {
        return getDeclaredFields(target, filter);
    }
    
    /**
     * Returns a set of all fields matching the supplied filter
     * declared in the clazz class.
     * 
     * @param clazz     The class to inspect.
     * @param filter    Filter to use.
     * 
     * @return All matching fields declared by the clazz class.
     **/
    private  Set<Field> getDeclaredFields(Class<?> clazz, FieldFilter filter) {
        Set<Field> fields = new HashSet<Field>();
        Field[] allFields = clazz.getDeclaredFields();
        for(Field field : allFields) {
            if(filter.passFilter(field)) {
                fields.add(field);
            }
        }
        return fields;
    }
    
    /**
     * Returns a set of all fields matching the supplied filter
     * declared in the target class or any of its super classes.
     * 
     * @param filter    Filter to use.
     * 
     * @return All matching fields declared by the target class.
     **/
    public Set<Field> getFields(FieldFilter filter) {
        Class<?> clazz = target;
        Set<Field> fields = getDeclaredFields(clazz, filter);
        while((clazz = clazz.getSuperclass()) != null) {    
            fields.addAll(getDeclaredFields(clazz, filter));
        }
        return fields;
    }
    
    /**
     * Returns set of constructors that matches the filter parameter.
     * 
     * @param filter Filter to apply.
     * @param ofType Class to get constructor for, must match target class.
     * 
     * @return constructors that matches the filter parameter.
     * 
     * @throws IllegalArgumentException if ofType does not match the target class.
     **/
    @SuppressWarnings("unchecked")
    public <T> Set<Constructor<T>> getConstructors(ConstructorFilter filter, Class<T> ofType) {
        if(!ofType.equals(target)) {
            throw new IllegalArgumentException("ofType must be target class: " + target);
        }
        Set<Constructor<T>> cons = new HashSet<Constructor<T>>();
        Constructor<T>[] constructors = (Constructor<T>[]) target.getDeclaredConstructors();
        for(Constructor<T> constructor : constructors) {
            if(filter.passFilter(constructor)) {
                cons.add(constructor);
            }
        }
        return cons;
    }
    
    /**
     * Returns set of constructors that matches the filter parameter.
     * 
     * @param filter Filter to apply.
     * 
     * @return constructors that matches the filter parameter.
     **/
    public Set<Constructor<?>> getConstructors(ConstructorFilter filter) {
        
        Set<Constructor<?>> cons = new HashSet<Constructor<?>>();
        Constructor<?>[] constructors = (Constructor<?>[]) target.getDeclaredConstructors();
        for(Constructor<?> constructor : constructors) {
            if(filter.passFilter(constructor)) {
                cons.add(constructor);
            }
        }
        return cons;
    }

    /**
     * Returns the first constructor found that matches the filter parameter.
     * 
     * @param filter Filter to apply.
     * @param ofType Class to get constructor for, must match target class.
     * 
     * @return the first constructor found that matches the filter parameter.
     * @throws IllegalArgumentException if ofType does not match the target class.
     * or no constructor is found matching the filter.
     **/
    public <T> Constructor<T> getConstructor(ConstructorFilter filter, Class<T> ofType) {
        if(!ofType.equals(target)) {
            throw new IllegalArgumentException("ofType must be target class: " + target);
        }
        Set<Constructor<T>> cons = getConstructors(filter, ofType);
        if(cons.isEmpty()) {
            throw new IllegalArgumentException("No constructor found mathcing filter " + filter);
        }
        return cons.iterator().next();
    }
    
    /**
     * Returns the first constructor found that matches the filter parameter.
     * 
     * @param filter Filter to apply.
     * @param ofType Class to get constructor for, must match target class.
     * 
     * @return the first constructor found that matches the filter parameter.
     * 
     * @throws IllegalArgumentException if no constructor is found matching the filter.
     **/
    public Constructor<?> getConstructor(ConstructorFilter filter) {
        Set<Constructor<?>> cons = getConstructors(filter);
        if(cons.isEmpty()) {
            throw new IllegalArgumentException("No constructor found mathcing filter " + filter);
        }
        return cons.iterator().next();
    }
    
    /**
     * Returns a map of methods annotated with an annotation from the annotations parameter.
     * 
     * @param methodFilter  Filter for methods, may be null to include all annotated methods.
     * @param annotations   Method annotations to find methods for
     * 
     * @return Methods that is annotated with the supplied annotation set.
     **/
    public Map<Class<? extends Annotation>, Set<Method>> findAnnotatedMethods(MethodFilter methodFilter, Collection<Class<? extends Annotation>> annotations) {
        
        Map<Class<? extends Annotation>, Set<Method>> annotatedMethods = new HashMap<Class<? extends Annotation>, Set<Method>>();
        for (Class<? extends Annotation> annotation : annotations) { 
            MethodFilter annotationFilter = new MethodFilterBuilder().annotated(annotation).build();
            if(methodFilter != null) {
                annotationFilter = new MethodFilterList(annotationFilter, methodFilter);
            }
            Set<Method> methods = getMethods(annotationFilter);
            annotatedMethods.put(annotation, methods);
        }
        
        return annotatedMethods;
    }
    
}
