package test;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import restdisp.worker.AbstractWorker;

public class Action extends AbstractWorker  {
	public Action(HttpServletRequest req, HttpServletResponse rsp) {
		super(req, rsp);
	}
	
	public void addUser(String name, long id, int cardId) throws IOException {
		System.out.println("addUser: " + name);
		getResponse().getWriter().write(name + id + cardId);
	}
	
	public void getUser(int id) throws IOException {
		System.out.println("getUser: " + id);
		getResponse().getWriter().write("" + id);
	}
	
	public void removeUser(int id) throws IOException {
		System.out.println("removeUser: " + id);
		getResponse().getWriter().write("" + id);
	}
	
	public void getUsers() throws IOException {
		System.out.println("getUsers");
		getResponse().getWriter().write("dummy");
	}
	
	public void getException() throws IOException {
		@SuppressWarnings("unused")
		int a = 5 / 0;
	}
}
