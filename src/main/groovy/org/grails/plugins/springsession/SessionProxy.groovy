package org.grails.plugins.springsession

import javax.servlet.http.HttpSession

interface SessionProxy {

	HttpSession getSession()
}
