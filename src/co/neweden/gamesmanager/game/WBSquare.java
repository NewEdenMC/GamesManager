package co.neweden.gamesmanager.game;

import org.bukkit.Location;

public class WBSquare {
	
	private Location centre;
	private int topX;
	private int topZ;
	private int bottomX;
	private int bottomZ;
	
	public WBSquare(Location centre, Integer radius) {
		this.centre = centre;
		topX = (int) centre.getX() + radius;
		topZ = (int) centre.getZ() + radius;
		bottomX = (int) centre.getX() - radius;
		bottomZ = (int) centre.getZ() - radius;
	}
	
	public WBSquare(Location centre, Integer topX, Integer topZ, Integer bottomX, Integer bottomZ) {
		this.centre = centre;
		this.topX = topX;
		this.topZ = topZ;
		this.bottomX = bottomX;
		this.bottomZ = bottomZ;
	}
	
	public void setCentre(Location centre) { this.centre = centre; }
	public Location getCentre() { return centre; }
	
	public void setTopX(Integer X) { topX = X; }
	public Integer getTopX() { return topX; }
	
	public void setTopZ(Integer Z) { topZ = Z; }
	public Integer getTopZ() { return topZ; }
	
	public void setBottomX(Integer X) { bottomX = X; }
	public Integer getBottomX() { return bottomX; }
	
	public void setBottomZ(Integer Z) { bottomZ = Z; }
	public Integer getBottomZ() { return bottomZ; }
	
}
