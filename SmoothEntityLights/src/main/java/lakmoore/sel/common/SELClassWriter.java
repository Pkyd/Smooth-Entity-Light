package lakmoore.sel.common;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class SELClassWriter extends ClassWriter {
	  public SELClassWriter( ClassReader classReader, int flags ) {
	    super( classReader, flags );
	  }

	  public SELClassWriter( int flags ) {
	    super( flags );
	  }

	  private ClassLoader classLoader;

	  public void setClassLoader( ClassLoader classLoader ) {
	    this.classLoader = classLoader;
	  }
	  
	  /*
	   * Exactly the same functionality as ObjectWeb's own getCommonSuperClass
	   * However, we now have control over which ClassLoader is used to 
	   * load the classes
	   */
	  @Override
	  protected String getCommonSuperClass( String type1, String type2 ) {
	    Class<?> c, d;

	    try {
	        c = Class.forName(type1.replace('/', '.'), false, classLoader);
	        d = Class.forName(type2.replace('/', '.'), false, classLoader);
	    } catch (Exception e) {
	        throw new RuntimeException(e.toString());
	    }
	    if (c.isAssignableFrom(d)) {
	        return type1;
	    }
	    if (d.isAssignableFrom(c)) {
	        return type2;
	    }
	    if (c.isInterface() || d.isInterface()) {
	        return "java/lang/Object";
	    } else {
	        do {
	            c = c.getSuperclass();
	        } while (!c.isAssignableFrom(d));
	        return c.getName().replace('.', '/');
	    }
	  }
	}
