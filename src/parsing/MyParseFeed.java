package parsing;

import java.util.ArrayList;
import java.util.HashMap;

import processing.core.PApplet;

public class MyParseFeed {
	
	// Name of each series contained within the Data.csv file
	
	// Population density is midyear population divided by land area in square kilometers.
	// Population is based on the de facto definition of population, which counts all residents regardless of legal status
	// or citizenship--except for refugees not permanently settled in the country of asylum, who are generally considered part of the population
	// of their country of origin. Land area is a country's total area, excluding area under inland water bodies, national claims to continental shelf,
	// and exclusive economic zones. In most cases the definition of inland water bodies includes major rivers and lakes.
	final static String DENSITY_SERIES = "Population density (people per sq. km of land area)";
	
	// Current expenditures on health per capita in current US dollars.
	// Estimates of current health expenditures include healthcare goods and services consumed during each year.
	final static String HEALTH_EXPENDITURE_PER_CAPITA_SERIES = "Current health expenditure per capita (current US$)";
	
	// Domestic General Government Health Expenditure (% Of Current Health Expenditure)
	// Share of current health expenditures funded from domestic public sources for health.
	// Domestic public sources include domestic revenue as internal transfers and grants, transfers, subsidies to
	// voluntary health insurance beneficiaries, non-profit institutions serving households (NPISH) or enterprise financing schemes
	// as well as compulsory prepayment and social health insurance contributions. They do not include external resources spent by governments on health.
	final static String PUBLIC_EXPENDITURE_HEALTH_SERIES = "Domestic general government health expenditure (% of current health expenditure)";
	
	// HashMap containing the name of the countries and their code (needed for loading the covid data)
	static HashMap<String, String> countryCodes = new HashMap<String, String>();
	
	/*
	 * This method is to parse a file containing different information from
	 * the world bank.
	 * 
	 * @param p - PApplet being used
	 * @param fileName - file name or URL for data source
	 * @return A HashMap of country->average age of death
	 */
	public static ArrayList<HashMap<String, Float>> loadData(PApplet p, String fileName) {
		// HashMap ArrayList containing data series
		ArrayList<HashMap<String, Float>> hashList = new ArrayList<HashMap<String,Float>>();
		// HashMap key: country ID and data: lifeExp at birth
		HashMap<String, Float> populationDensity = new HashMap<String, Float>();
		HashMap<String, Float> healthPerCapita = new HashMap<String, Float>();
		HashMap<String, Float> publicHealth = new HashMap<String, Float>();

		hashList.add(populationDensity);
		hashList.add(healthPerCapita);
		hashList.add(publicHealth);

		// get lines of csv file
		String[] rows = p.loadStrings(fileName);
		
		// Reads rows
		for (int row = 0; row < rows.length; row++) {
			if(row == 0) continue; // first row contains columns names
			
			// split row by commas not in quotations
			String[] columns = rows[row].split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			if(columns.length == 0) break;  // we are done
			
			// the data.csv file contains numerous series, this variable keeps track of which series we are currently checking
			String currentSeries = columns[2];
			
			float avg_density = 0;
			int densityCount = 0;
			float avg_healthPerCapita = 0;
			int healthPerCapitaCount = 0;
			float avg_publicHealth = 0;
			int publicHealthCount = 0;
			
			for(int i = columns.length - 1; i > 3; i--) {
				if(i > 3) {
					// calculate the average value for the data period. Note that not every country has the same range of data (sadly).
					// The algorithm will calculate the average for whatever range of years at our disposal
					if(columns[i].equals("..")) continue;
					Float value = Float.valueOf(columns[i]);
					if(currentSeries.equals(DENSITY_SERIES)) {
						densityCount++;
						avg_density += value;
					}else if(currentSeries.equals(HEALTH_EXPENDITURE_PER_CAPITA_SERIES)) {
						healthPerCapitaCount++;
						avg_healthPerCapita += value;
					}else if(currentSeries.equals(PUBLIC_EXPENDITURE_HEALTH_SERIES)) {  // could have used "else" but I may add new series
						publicHealthCount++;
						avg_publicHealth += value;
					}
				}
			}
			String countryID = columns[1];
			String country = columns[0];
			
			// add country to countryCodes map
			if(!countryCodes.containsKey(country)) countryCodes.put(country, countryID);
			
			if(currentSeries.equals(DENSITY_SERIES)) {
				if(densityCount == 0) densityCount = 1;
				avg_density = avg_density/densityCount;
				populationDensity.put(countryID, avg_density);
			}else if(currentSeries.equals(HEALTH_EXPENDITURE_PER_CAPITA_SERIES)) {
				if(healthPerCapitaCount == 0) healthPerCapitaCount = 1;
				avg_healthPerCapita = avg_healthPerCapita/healthPerCapitaCount;
				healthPerCapita.put(countryID, avg_healthPerCapita);
			}else if(currentSeries.equals(PUBLIC_EXPENDITURE_HEALTH_SERIES)) {
				if(publicHealthCount == 0) publicHealthCount = 1;
				avg_publicHealth = avg_publicHealth/publicHealthCount;
				publicHealth.put(countryID, avg_publicHealth);
			}
		}

		return hashList;
	}
	
	/**
	 * Loads the covid data. We are interested in the columns Cumulative_deaths and Cumulative_cases.
	 * Since each country contains many days of data, we will get only the last day.
	 * @param fileName
	 * @return
	 */
	public static ArrayList<HashMap<String, Float>> loadCovidData(PApplet p, String fileName){
		ArrayList<HashMap<String, Float>> mapList = new ArrayList<HashMap<String,Float>>();
		
		HashMap<String, Float> deathsMap = new HashMap<String, Float>();
		HashMap<String, Float> casesMap = new HashMap<String, Float>();
		
		mapList.add(deathsMap);
		mapList.add(casesMap);
		
		// get lines of csv file
		String[] rows = p.loadStrings(fileName);

		// Reads rows
		for (int row = 0; row < rows.length; row++) {
			if(row == 0) continue;
			
			// split row by commas not in quotations
			String[] columns = rows[row].split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			
			String country = columns[2];
			if(row < rows.length - 1) {
				// if next row begins another country, get data
				if((row < rows.length - 1 && !rows[row + 1].split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")[2].equals(country))
						|| row == rows.length - 1) { // next row is a different country
					String countryCode = countryCodes.get(country);
					if(countryCode == null) {  // this country id isn't present in the dataset from world bank
						countryCode = countryCodes.get(solveMismatch(country));  // try to fix it
						if(countryCode == null) {
							//System.out.println("Mismatch: " + country);
							continue;  // add more solutions to solveMismatch, or perhaps this country simply isn't in the other dataset
						}
					}
					deathsMap.put(countryCode, Float.valueOf(columns[7]));
					casesMap.put(countryCode, Float.valueOf(columns[5]));
				}
			}
		}
		
		return mapList;
	}
	
	/**
	 * Because I've used data from two different sources, it happens that the country IDs don't always match.
	 * This method manually fixes some of those differences. There are more, but since this project isn't about data cleaning
	 * nor data processing, I'm going to fix only some of them.
	 * @param country The full name of the country
	 * @return
	 */
	private static String solveMismatch(String country) {
		String newName = new String();
		if(country.contains("Saint")) {
			newName = country.replace("Saint", "St.");
		}else if(country.contains("United States of America")){
			newName = "United States";
		}else if(country.contains("United Kingdom")) {
			newName = "United Kingdom";
		}else if(country.contains("Venezuela")) {
			newName = "Venezuela, RB";
		}else if(country.contains("Viet Nam")) {
			newName = "Vietnam";
		}
		
		return newName;
	}
}
