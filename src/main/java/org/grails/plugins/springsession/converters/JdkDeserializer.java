package org.grails.plugins.springsession.converters;

import org.springframework.core.serializer.Deserializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jitendra
 */
public class JdkDeserializer implements Deserializer<Object> {

    private static final String OLD_PACKAGE_NAME = "org.codehaus.groovy.grails";
    private static final String NEW_INTERNAL_PACKAGE = "org.grails";
    private static final String NEW_PUBLIC_PACKAGE = "grails";

    // Known chages between Grails 2 and 3
    private static final Map<String, String> EQUIVALENT_CLASSES;

    static {
        EQUIVALENT_CLASSES = new HashMap<>();
        mapClasses("org.springframework.security.core.authority.GrantedAuthorityImpl",
                "org.springframework.security.core.authority.SimpleGrantedAuthority");
        mapClasses("org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser",
                "grails.plugin.springsecurity.userdetails.GrailsUser");
    }

    private ClassLoader classLoader;

    private Boolean instantiate;

    public JdkDeserializer(ClassLoader classLoader, Boolean instantiate) {
        this.classLoader = classLoader;
        this.instantiate = instantiate;
    }

    private static void mapClasses(String class1, String class2){
        EQUIVALENT_CLASSES.put(class1, class2);
        EQUIVALENT_CLASSES.put(class2, class1);
    }

    @Override
    public Object deserialize(InputStream inputStream) throws IOException {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                String name = desc.getName();
                try {
                    return Class.forName(desc.getName(), instantiate, classLoader);
                } catch (ClassNotFoundException e) {
                    try {
                        return super.resolveClass(desc);
                    } catch (ClassNotFoundException e2) {
                        throw e;
                    }
                }
            }

            // Custom implementation that use the local class description instead of the one which comes
            // from the stream. It means that it'll ignore differences in serialVersionUID.
            // It also tries to find an equivalent class, in case a different version of Grails
            // serialized the object
            @Override
            protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                ObjectStreamClass classDescriptor = super.readClassDescriptor(); // initially streams descriptor
                Class localClass; // the class in the local JVM that this descriptor represents.
                String name = classDescriptor.getName();
                try {
                    localClass = Class.forName(name);
                } catch (ClassNotFoundException e) {
                    // local class not found, searching for equivalents...
                    localClass = searchEquivalentClass(name);
                    if(localClass == null){
                        throw e;
                    }
                }
                return ObjectStreamClass.lookup(localClass);
            }
        };

        try {
            return objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private Class<?> searchEquivalentClass(String name) {
        // Known class that changed between Grails 2 and 3?
        String equivalentClass = EQUIVALENT_CLASSES.get(name);
        if(equivalentClass != null){
			try{
				return Class.forName(equivalentClass, instantiate, classLoader);
			}
			catch (ClassNotFoundException e){}
		}

        // Was it serialized with an older version of Grails?
        if(name.contains(OLD_PACKAGE_NAME)){
			// Try the new internal package
			try{
				return Class.forName(name.replace(OLD_PACKAGE_NAME, NEW_INTERNAL_PACKAGE), instantiate, classLoader);
			}
			catch (ClassNotFoundException e){}
			// Try the new public package
			try{
				return Class.forName(name.replace(OLD_PACKAGE_NAME, NEW_PUBLIC_PACKAGE), instantiate, classLoader);
			}
			catch (ClassNotFoundException e){}
		}
		// Was it serialized with a newer version of Grails (internal package)?
		else if(name.contains(NEW_INTERNAL_PACKAGE)){
			// Try the old package
			try{
				return Class.forName(name.replace(NEW_INTERNAL_PACKAGE, OLD_PACKAGE_NAME), instantiate, classLoader);
			}
			catch (ClassNotFoundException e){}
		}
		// Was it serialized with a newer version of Grails (public package)?
		else if(name.contains(NEW_PUBLIC_PACKAGE)){
			// Try the old package
			try{
				return Class.forName(name.replace(NEW_PUBLIC_PACKAGE, OLD_PACKAGE_NAME), instantiate, classLoader);
			}
			catch (ClassNotFoundException e){}
		}
        return null;
    }
}
