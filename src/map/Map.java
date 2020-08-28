package map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.MyParseFeed;
import processing.core.PApplet;

/**
 * Map that shows four different datasets as colors over countries (from red -bad- to blue -good-). These datasets are explained in more detail in
 * the class MyParseFeed.java
 * 
 * The user can click on the different buttons to change from one dataset representation to any other at any moment.
 * 
 * Disclaimer: This project doesn't intend to accurately represent the data.
 * 
 * @author antonio baena
 * @see MyParseFeed
 *
 */
public class Map extends PApplet{
	UnfoldingMap map;
	HashMap<String, Float> populationDensityMap, healthPerCapitaMap, publicHealthMap, deathsMap, casesMap;
	List<HashMap<String, Float>> myMaps = new ArrayList<HashMap<String, Float>>();
	
	List<Feature> countries;
	List<Marker> countryMarkers;
	List<Marker> dataMarkers;
	
	Marker lastClicked;

	// Buttons locations
	private int initialx = 50;
	private int initialy = 20;
	private int buttonSize = 20;
	private int buttonMargin = 0;
	private int textxPadding = 25;
	private int textyPadding = 15;
	private int columnSize = 300;
	// To add more buttons, simply add here another String to the array. Everything else will be calculated automatically
	// Note that the maximum number of buttons with the current layout is roughly 8
	String[] buttonsText = {"Population density",
			"Health per capita expenses",
			"Public health expenses over total",
			"Covid-19 total cases",
			"Covid-19 total deaths"};

	public void setup() {
		// I have been having a lot of trouble with my linux computer with Intel Corporation HD Graphics 630 card.
		// For some reason (a bug, I think) I can't use OPENGL. I found out that writing this line below I could, at least, make 
		// the code work (I had to use this line in every module). Remove it if not necessary for you.
		System.setProperty("jogl.disable.openglcore", "false");
		
		size(1200, 800, OPENGL);
		map = new UnfoldingMap(this, 50, 80, 1100, 700, new Google.GoogleMapProvider());
		MapUtils.createDefaultEventDispatcher(this, map);
		map.zoom(2f);

		// Load data
		List<HashMap<String, Float>> mapsList = MyParseFeed.loadData(this,"data.csv");
		populationDensityMap = mapsList.get(0);
		healthPerCapitaMap = mapsList.get(1);
		publicHealthMap = mapsList.get(2);
		
		List<HashMap<String, Float>> covidList = MyParseFeed.loadCovidData(this, "covid.csv");
		deathsMap = covidList.get(0);
		casesMap = covidList.get(1);
		
		myMaps.add(populationDensityMap);
		myMaps.add(healthPerCapitaMap);
		myMaps.add(publicHealthMap);
		myMaps.add(deathsMap);
		myMaps.add(casesMap);

		// Load country polygons and adds them as markers
		countries = GeoJSONReader.loadData(this, "countries.geo.json");
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		map.addMarkers(countryMarkers);
		
		dataMarkers = new ArrayList<Marker>();
		for(Marker country:countryMarkers) {
			boolean allPresent = true;
			for(HashMap<String, Float> map:myMaps) {
				if(!map.containsKey(country.getId())) {
					allPresent = false;
					break;
				}
			}
			if(!allPresent) continue;
			
			String countryId = country.getId();
			if(populationDensityMap.containsKey(countryId)) {
				DataMarker dm = new DataMarker(country.getLocation(), country.getId(), (String)country.getProperty("name"),
						populationDensityMap.get(countryId), healthPerCapitaMap.get(countryId),
						publicHealthMap.get(countryId), deathsMap.get(countryId), casesMap.get(countryId));
				dataMarkers.add(dm);
			}
		}
		map.addMarkers(dataMarkers);
	}
	
	@Override
	public void mouseClicked() {
		checkButtonClick();
		checkCountryClick();
		
	}
	
	private void checkButtonClick() {
		for(int i = 0; i < buttonsText.length; i++) {
			float x = initialx + columnSize * (int)(i / 2);
			float y = initialy * (i % 2 == 0 ? 1 : 2) + buttonMargin * (i % 2 == 0 ? 0 : 1);
			if(mouseX > x && mouseX < x + buttonSize
					&& mouseY > y && mouseY < y + buttonSize) {
				if(i == 0) {
					// shade by population density 
					float[] minMaxValues = findMinMaxValues(populationDensityMap);
					shadeCountries(populationDensityMap, minMaxValues[0], minMaxValues[1], minMaxValues[2], false);
					return;
				}else if(i == 1) {
					// shade by health per capita
					float[] minMaxValues = findMinMaxValues(healthPerCapitaMap);
					shadeCountries(healthPerCapitaMap, minMaxValues[0], minMaxValues[1], minMaxValues[2], true);
					return;
				}else if(i == 2) {
					float[] minMaxValues = findMinMaxValues(publicHealthMap);
					shadeCountries(publicHealthMap, minMaxValues[0], minMaxValues[1], minMaxValues[2], true);
					return;
				}else if(i == 3) {
					float[] minMaxValues = findMinMaxValues(casesMap);
					shadeCountries(casesMap, minMaxValues[0], minMaxValues[1], minMaxValues[2], false);
					return;
				}else if(i == 4) {
					float[] minMaxValues = findMinMaxValues(deathsMap);
					shadeCountries(deathsMap, minMaxValues[0], minMaxValues[1], minMaxValues[2], false);
					return;
				}
			}
		}
	}
	
	private void checkCountryClick() {
		// Check for clicks on countries
		if(lastClicked != null) lastClicked.setHidden(true);
		 Marker marker = map.getFirstHitMarker(mouseX, mouseY);
		  if (marker != null) {
			  for(Marker dataMarker:dataMarkers) {
				  if(((DataMarker)dataMarker).getCountryId().equals(marker.getId())) {
					  if(lastClicked != dataMarker) {
						  dataMarker.setHidden(false);
						  lastClicked = dataMarker;
						  break;
					  }else {
						  lastClicked = null;
					  }
				  }
			  }
		  }
	}

	public void draw() {
		background(100);
		map.draw();
		addKey();
	}
	
	// helper method to draw key in GUI
	private void addKey() {
		// Create buttons and text
		for(int i = 0; i < buttonsText.length; i++) {
			if(i != 0) buttonMargin = 5;
			fill(150,150,150);
			stroke(0,0,0);
			rect(initialx + columnSize * (int)(i / 2), initialy * (i % 2 == 0 ? 1 : 2) + buttonMargin * (i % 2 == 0 ? 0 : 1), buttonSize, buttonSize);
			fill(255,255,255);
			text(buttonsText[i], initialx + textxPadding + columnSize * (int)(i / 2), initialy * (i % 2 == 0 ? 1 : 2) + buttonMargin * (i % 2 == 0 ? 0 : 1) + textyPadding);
		}
		
		// Create the color key next to the map
		addColorKey();
	}
	
	/**
	 * Adds a bar at the left side of the map showing the color key (blue means good, red bad)
	 */
	private void addColorKey() {
		// Color bar
		int barLength = 670;
		setGradient(15, 95, 20, barLength / 2, color(0,0,255), color(255,255,0));
		setGradient(15, 95 + barLength / 2, 20, barLength / 2, color(255,255,0), color(255,0,0));
		stroke(color(0,0,0));
		rect(15, 95, 20, barLength);
		text("Better", 7, 90);
		text("Worse", 7, 780);
	}
	
	private void setGradient(int x, int y, float w, float h, int c1, int c2) {
	  noFill();
	  for (int i = y; i <= y + h; i++) {
	      float inter = map(i, y, y + h, 0, 1);
	      stroke(lerpColor(c1, c2, inter));
	      line(x, i, x + w, i);
	    }
	}

	/**
	 * This method shades countries depending on the selected dataset. Since I found many outliers in the data (extreme values that distorted the results),
	 * I decided to eliminate the values that are above or below the mean by two times the standard deviation, and apply this filter twice. Again, as stated before,
	 * not that this is the best method to remove outliers, but since this isn't about data cleaning, I simply tuned it until I got some useful and decent results.
	 * 
	 * I also decided to split the data representation into two categories: countries that are above the mean and those that are below it. Countries below the mean
	 * will range from red to yellow, whereas countries above the mean will range from yellow to blue. So, instead of ranging from red to blue, countries range from
	 * red to yellow and to blue (wider range, finer representation).
	 * 
	 * @param dataMap
	 * @param minValue Calculated in method findMinMaxValues
	 * @param maxValue Calculated in method findMinMaxValues
	 * @param higherIsBetter true if higher values in the dataset are a good thing (for example, higher values in total covid deaths isn't a good thing, so
	 * it should be set to false, whereas health expenses per capita should be set to true)
	 */
	private void shadeCountries(HashMap<String, Float> dataMap, float minValue, float maxValue, float mean, boolean higherIsBetter) {
		for (Marker marker : countryMarkers) {
			// Find data for country of the current marker
			String countryId = marker.getId();
			if (dataMap.containsKey(countryId)) {
				//System.out.println(minValue + ", " + maxValue);
				//System.out.println(marker.getProperty("name"));
				//System.out.println("Media: " + median);
				if(higherIsBetter) {
					float colorLevel;
					if(dataMap.get(countryId) >= mean) {
						colorLevel = (dataMap.get(countryId) - mean) / (maxValue - mean);
						//System.out.println("Above media: " + colorLevel);
						//System.out.println("Country value: " + dataMap.get(countryId));
						marker.setColor(color(255 * (1 - colorLevel), 255 * (1 - colorLevel), 255 * colorLevel));
					}else {
						colorLevel = (mean - dataMap.get(countryId))/(mean - minValue);
						//System.out.println("Below media: " + colorLevel);
						//System.out.println("Country value: " + dataMap.get(countryId));
						marker.setColor(color(255, 255 * (1 - colorLevel), 0));
					}
				}else {
					float colorLevel;
					if(dataMap.get(countryId) <= mean) {
						colorLevel = (mean - dataMap.get(countryId)) / (mean - minValue);
						//System.out.println("Below media: " + colorLevel);
						//System.out.println("Country value: " + dataMap.get(countryId));
						marker.setColor(color(255 * (1 - colorLevel), 255 * (1 - colorLevel), 255 * colorLevel));
					}else {
						colorLevel = ((dataMap.get(countryId) - mean)) / (maxValue - mean);
						if(colorLevel > 1) colorLevel = 1;
						//System.out.println("Above media: " + colorLevel);
						//System.out.println("Country value: " + dataMap.get(countryId));
						marker.setColor(color(255, 255 * (1 - colorLevel), 0));
					}
				}
				//System.out.println();
			}
			else {
				marker.setColor(color(0,0,0));
			}
		}
	}
	
	/**
	 * Removes some outliers and returns the mean, max and min values (once outliers have been removed). I needed to do this
	 * because there were so many extreme values that every country ended up being either completely blue, or completely red, with only two
	 * or three different shades in the whole map.
	 * This is only logical since wealth distribution is very different in the first, second and third world. Another possible solution would
	 * have been to colorize data differently for each world, but I chose to go with this. Feel free to test new ways.
	 * 
	 * @param map
	 * @return
	 */
	private float[] findMinMaxValues(HashMap<String, Float> map) {
		float minMaxValues[] = new float[3];
		
		// Create a copy of the map to work on because we will remove some keys
		HashMap<String, Float> copyMap = new HashMap<String, Float>();
		for(String key:map.keySet()) copyMap.put(key, map.get(key));
		
		float mean = 0;
		float std = 0;
		ArrayList<String> keysRemove = new ArrayList<String>();
		
		// go through the dataset a number of times to remove outliers
		// two or three iterations should be enough
		do{
			mean = calculateMean(copyMap);
			std = calculateStandardDeviation(copyMap, mean);
			//System.out.println("mean: " + mean);
			//System.out.println("std: " + std);
			//System.out.println("diff: " + abs(mean - std));
			for(String key:copyMap.keySet()) {
				if(abs(copyMap.get(key) - mean) > 2 * std) keysRemove.add(key);
			}
			for(String key:keysRemove) {
				//System.out.println("Removing a key: " + key);
				copyMap.remove(key);
			}
			//System.out.println();
		}while(2 * mean < std);
		
		//System.out.println("final mean: " + mean);
		//System.out.println("final std: " + std);
		float minValue = Float.MAX_VALUE;
		float maxValue = 0;
		
		for(String key:map.keySet()) {
			if(abs(map.get(key) - mean) > std || map.get(key) == 0) {
				continue;
			}
			if(map.get(key) < minValue) minValue = map.get(key);
			if(map.get(key) > maxValue) maxValue = map.get(key);
		}
		
		minMaxValues[0] = minValue;
		minMaxValues[1] = maxValue;
		minMaxValues[2] = mean;
		
		return minMaxValues;
	}
	
	private float calculateStandardDeviation(HashMap<String, Float> map, float mean) {
		
		float variance = calculateVariance(map, mean);
		
		return (float)Math.sqrt(variance);
	}
	
	private float calculateMean(HashMap<String, Float> map) {
		float mean = 0;
		int count = 0;
		
		for(String key:map.keySet()) {
			if(map.get(key) == 0) {
				continue;
			}
			mean += map.get(key);
			count++;
		}
		mean = mean / count;
		return mean;
	}
	
	private float calculateVariance(HashMap<String, Float> map, float mean) {
		float variance = 0;
		int count = 0;
		
		for(String key:map.keySet()) {
			if(map.get(key) == 0) {
				continue;
			}
			variance += Math.pow(map.get(key) - mean, 2);
			count++;
		}
		
		variance = variance / count;
		
		return variance;
	}
}
