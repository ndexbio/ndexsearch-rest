package org.ndexbio.ndexsearch.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GeneValidator {
	
	private TreeSet<String> geneSymbolSet;
	private TreeMap<String,String> aliasMap;
	

	public GeneValidator(File file) {
		geneSymbolSet = new TreeSet<>();
		aliasMap = new TreeMap<>();
		
		long counter = 0;
	    try (Scanner sc = new Scanner(file)){
	        String line;
	        String[] cells;
	        while (sc.hasNextLine()) {
	              line = sc.nextLine();
	        	  counter ++;
	        	  if ( counter == 1)
	        		  continue;
	              // process the line
	              cells=line.split("\t");
	              if ( cells[0] != null) {
	            	  String gene= cells[0]; //.toUpperCase();
	            	  geneSymbolSet .add(gene);
	              
	            	  if ( cells.length >1 && cells[1].length()>0) {
	            		  String[] alias= cells[1].split(",\\s*");
	            		  for ( String a : alias) {
	            			  aliasMap.put(a, gene);
	            		  }
	            	  }
	            	  if ( cells.length >2 && cells[2].length()>0) {
	            		  String[] alias= cells[2].split(",\\s*");
	            		  for ( String a : alias) {
	            			  aliasMap.put(a, gene);
	            		  }
	            	  }
	              }
	        }
	    } catch(FileNotFoundException e) {
	              e.printStackTrace();
	    } 	
	}
	
	public TreeSet<String> getGeneSymbolSet() {return geneSymbolSet;}
	public TreeMap<String,String> getAliasMap() {return aliasMap;}
	
	public Map<String,Object> validateHumanGenes(Collection<String> genes) {
		Map<String,Object> result = new HashMap<>();
		
		Set<String> officialGenes = new TreeSet<>();
		Set<String> invalidGenes = new TreeSet<>();
		
		//Mapping from official symbols to alias
		Map<String,String> normalizedGenes = new TreeMap<>();
		
		result.put("queryGenes", officialGenes);
		result.put("invalid", invalidGenes);
		result.put("normalized",normalizedGenes);
		
		for ( String term : genes) {
			//String capTerm = term.toUpperCase();
			if (geneSymbolSet.contains(term)) {
				officialGenes.add(term);
			} else if ( aliasMap.containsKey(term)) {
				String officialGene = aliasMap.get(term);
				officialGenes.add(officialGene);
				normalizedGenes.put(officialGene, term);
			} else 
				invalidGenes.add(term);
				
			
		}
        return result;			
	}
	
}
