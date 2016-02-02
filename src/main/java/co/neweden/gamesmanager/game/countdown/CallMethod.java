package co.neweden.gamesmanager.game.countdown;

public class CallMethod {
	
	private Object instance;
	private String method;
	
	CallMethod(Object instance, String method) {
		this.instance = instance;
		this.method = method;
	}
	
	public Object getInstance() { return this.instance; }
	public String getMethod() { return this.method; }
	
}
