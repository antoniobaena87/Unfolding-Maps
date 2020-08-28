package map;

import de.fhpotsdam.unfolding.geo.Location;
import processing.core.PGraphics;

public class DataMarker extends CommonMarker{
	
	String countryId, countryName;
	float density, healthPerCapita, publicHealth, deaths, totalCases;
	
	public DataMarker(Location location, String countryId, String countryName, float density, float healthPerCapita, float publicHealth, float deaths, float totalCases) {
		super(location);
		this.countryId = countryId;
		this.countryName = countryName;
		this.density = density;
		this.healthPerCapita = healthPerCapita;
		this.publicHealth = publicHealth;
		this.deaths = deaths;
		this.totalCases = totalCases;
		
		setHidden(true);
	}
	
	public float getDeaths() {
		return deaths;
	}
	
	@Override
	public void drawMarker(PGraphics pg, float x, float y) {

	}
	
	@Override
	public void showTitle(PGraphics pg, float x, float y) {
		pg.fill(255,255,255);
		pg.rect(x, y, 250, 120);
		
		pg.fill(0,0,0);
		pg.text(printData(), x + 20, y + 20);
	}
	
	private String printData() {
		return countryName + 
				"\n\nPopulation density: " + density + 
				"\nHealth per capita: " + healthPerCapita +
				"\nPublic health: " + publicHealth +
				"\nCovid total cases: " + totalCases +
				"\nCovid total deaths: " + deaths;
	}
	
	public String getCountryId() {
		return countryId;
	}
}
