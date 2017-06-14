package org.grails.plugins.springsession.scope;

import org.grails.plugins.springsession.SpringHttpSession;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.context.request.DestructionCallbackBindingListener;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;

public class SpringSessionScope extends SessionScope {

	private BeanFactory beanFactory;

	public SpringSessionScope(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object get(String name, ObjectFactory objectFactory) {
		SpringHttpSession session = beanFactory.getBean(SpringHttpSession.class);

		Object object = session.getAttribute(name);
		if (object == null) {
			object = objectFactory.getObject();
			session.setAttribute(name, object);
		}

		return object;
	}

	@Override
	public Object remove(String name) {
		SpringHttpSession session = beanFactory.getBean(SpringHttpSession.class);

		Object object = session.getAttribute(name);
		if (object != null) {
			session.removeAttribute(name);
			return object;
		} else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		SpringHttpSession session = beanFactory.getBean(SpringHttpSession.class);

		session.setAttribute(ServletRequestAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX + name, new DestructionCallbackBindingListener(callback));
	}
}
