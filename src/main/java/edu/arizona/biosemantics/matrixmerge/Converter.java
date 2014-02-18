package edu.arizona.biosemantics.matrixmerge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * This assumes 
 * (1) the inputFile be a reasonable matrix in CSV format, i.e. it has a header row and the same number of columns over all rows
 * (2) "from" and "to" columns to have a specific suffix
 * (3) a "from" column can always be matched to a corresponding "to" column by removing the suffixes
 */
public class Converter {
	
	private String toSuffix = "_to";
	private String fromSuffix = "_from";
	private String inputFile = "matrix.csv";
	private String outputFile = inputFile + "_out.csv";
	
	private Map<Integer, String> fromCharacters = new LinkedHashMap<Integer, String>();
	private Map<Integer, String> toCharacters = new LinkedHashMap<Integer, String>();
	private Map<String, Integer> reverseFromCharacters = new LinkedHashMap<String, Integer>();
	private Map<String, Integer> reverseToCharacters = new LinkedHashMap<String, Integer>();
	private Map<Integer, List<FromToValue>> fromToValues = new LinkedHashMap<Integer, List<FromToValue>>();
	
	private void convert() throws Exception {
		fromCharacters.clear();
		toCharacters.clear();
		reverseFromCharacters.clear();
		reverseToCharacters.clear();
		
		List<String[]> input = readInput();		
		List<String[]> output = createOutput(input);
		writeOutput(output);
	}
	

	private List<String[]> createOutput(List<String[]> input) throws Exception {
		initialize(input);
		checkAssumptions();
		List<String[]> output = merge(input);
		return output;
	}

	private List<String[]> merge(List<String[]> input) {
		List<String[]> output = new LinkedList<String[]>();
		
		int numberOfInputColumns = input.get(0).length;
		int numberOfOutputColumns = numberOfInputColumns - reverseToCharacters.size();
		
		//create header
		String[] header = new String[numberOfOutputColumns];
		int outputColumn = 0;
		String[] inputCells = input.get(0);
		for(int inputColumn = 0; inputColumn < inputCells.length; inputColumn++) {
			if(!fromCharacters.containsKey(inputColumn) && !toCharacters.containsKey(inputColumn)) {
				header[outputColumn++] = inputCells[inputColumn];
			} else if(fromCharacters.containsKey(inputColumn)) {
				header[outputColumn++] = fromCharacters.get(inputColumn);
			}
		}
		output.add(header);
		
		//create content
		for(int row = 1; row < input.size(); row++) {
			String[] outputCells = new String[numberOfOutputColumns];
			output.add(outputCells);

			outputColumn = 0;
			inputCells = input.get(row);
			try {
				if(inputCells.length != numberOfInputColumns) {
					throw new Exception("Assumption not met: Row " + row + " contains unequal amount of values than header");
				}
				for(int inputColumn = 0; inputColumn < inputCells.length; inputColumn++) {
					if(!fromCharacters.containsKey(inputColumn) && !toCharacters.containsKey(inputColumn)) {
						outputCells[outputColumn++] = inputCells[inputColumn];
					} else if(fromCharacters.containsKey(inputColumn)) {
						outputCells[outputColumn++] = getMergedValue(fromToValues.get(inputColumn).get(row-1));
					}
				}
			} catch(Exception e) {
				System.out.println("Will leave out this row for now");
				e.printStackTrace();
			}
		}
		return output;
	}


	private void checkAssumptions() throws Exception {
		//check assumptions about matrix input format
		if(reverseFromCharacters.size() != reverseToCharacters.size()) 
			throw new Exception("Assumptions not met: Unequal amount of from and to");
		//for(Integer from : fromCharacters.keySet()) {
			//if(!toCharacters.containsKey(from + 1))
			//	throw new Exception("Assumption not met: from not immediately followed by a to");
			//String fromCharacter = fromCharacters.get(from);
			//String toCharacter = toCharacters.get(from + 1);
			//fromCharacter = fromCharacter.substring(0, fromCharacter.lastIndexOf(fromSuffix));
			//toCharacter = toCharacter.substring(0, toCharacter.lastIndexOf(toSuffix));
			//if(!fromCharacter.equals(toCharacter))
			//	throw new Exception("Assumption not met: from and to character do not match");
		//}
		for(String character : reverseFromCharacters.keySet()) {
			if(!reverseToCharacters.containsKey(character)) {
				throw new Exception("Assumption not met: from and to character do not match");
			}
		}
	}


	private void initialize(List<String[]> input) {
		//initialize
		String[] line = input.get(0);
		for(int i=0; i<line.length; i++) {
			String character = line[i];
			if(character.endsWith(fromSuffix)) {
				//fromToValues.put(i, new LinkedList<FromToValue>());
				String nonRangeCharacter = character.substring(0, character.lastIndexOf(fromSuffix));
				fromCharacters.put(i, nonRangeCharacter);
				reverseFromCharacters.put(nonRangeCharacter, i);
			}
			if(character.endsWith(toSuffix)) {
				String nonRangeCharacter = character.substring(0, character.lastIndexOf(toSuffix));
				toCharacters.put(i, nonRangeCharacter);
				reverseToCharacters.put(nonRangeCharacter, i);
			}
		}
		
		//_from column id to list of fromToValues over rows
		for(int row = 1; row<input.size(); row++) {
			String[] cells = input.get(row);
			
			for(String character : reverseFromCharacters.keySet()) {
				int fromIndex = reverseFromCharacters.get(character);
				int toIndex = reverseToCharacters.get(character);
				if(!fromToValues.containsKey(fromIndex))
					fromToValues.put(fromIndex, new LinkedList<FromToValue>());
				fromToValues.get(fromIndex).add(new FromToValue(cells[fromIndex], cells[toIndex]));
			}
		}
	}


	private String getMergedValue(FromToValue fromToValue) {
		String fromValue = fromToValue.getFromValue();
		String toValue = fromToValue.getToValue();
		
		Set<String> overallItems = new HashSet<String>();
		Set<String> fromItems = new HashSet<String>(Arrays.asList(fromValue.split("\\|")));
		Set<String> toItems = new HashSet<String>(Arrays.asList(toValue.split("\\|")));
		overallItems.addAll(fromItems);
		overallItems.addAll(toItems);
		
		if(overallItems.size() == fromItems.size() && overallItems.size() == toItems.size()) {
			String result = "";
			for(String item : overallItems) {
				result += item + "|";
			}
			return result.substring(0, result.length() - 1);
		}

		if(fromValue.isEmpty() && toValue.isEmpty())
			return "";
		if(fromValue.trim().equals(toValue.trim())) 
			return fromValue;
		return fromValue + " -> " + toValue;
	}

	private List<String[]> readInput() throws IOException {
		InputStream inputStream = new FileInputStream(inputFile);
		CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStream, "UTF8")), '\t');
		List<String[]> lines = reader.readAll();	
	    reader.close();	
		return lines;
	}

	private void writeOutput(List<String[]> output) throws IOException {
		OutputStream outputStream = new FileOutputStream(outputFile);
	    CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF8")), '\t', CSVWriter.NO_QUOTE_CHARACTER);
	    writer.writeAll(output);
	    writer.close();
	}

	
	
	public static void main(String[] args) throws Exception {
		Converter converter = new Converter();
		converter.convert();
	}


}
