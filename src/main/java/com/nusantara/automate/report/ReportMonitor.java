package com.nusantara.automate.report;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ReportMonitor {

	
	private static LinkedList<TestCaseEntry> testCaseEntries = new LinkedList<TestCaseEntry>();
	private static Map<String, LinkedList<ScenEntry>> scenEntries = new HashMap<String, LinkedList<ScenEntry>>();  
	private static Map<String, ScenEntry> scenEntriesByTscenId = new HashMap<String, ScenEntry>();
	private static LinkedHashMap<String, String> dataEntryBySessionId = new LinkedHashMap<String, String>();
	private static Map<String, LinkedList<DataEntry>> dataEntries = new HashMap<String, LinkedList<DataEntry>>();
	private static Map<String, LinkedList<ImageEntry>> imageEntries = new HashMap<String, LinkedList<ImageEntry>>();
	
	public static void addTestCaseEntry(TestCaseEntry testCaseEntry, LinkedList<ScenEntry> scenEntryList) {
		for (ScenEntry scenEntry : scenEntryList) {
			scenEntriesByTscenId.put(scenEntry.getTscanId(), scenEntry);			
		}
		testCaseEntries.add(testCaseEntry);
		scenEntries.put(testCaseEntry.getTestCaseId(), scenEntryList);
	}
	
	public static ScenEntry getScenEntry(String tscenId) {
		return scenEntriesByTscenId.get(tscenId);
	}
	
	public static TestCaseEntry getTestCaseEntry(String testCaseId) {
		for (TestCaseEntry testCase : testCaseEntries) {
			if (testCase.getTestCaseId().equals(testCaseId)) {
				return testCase;
			}
		}
		return null;
	}
	
	public static ScenEntry getScenEntry(String testCaseId, String scenId) {
		for (ScenEntry scenEntry : scenEntries.get(testCaseId)) {
			if (scenEntry.getTscanId().equals(scenId)) {
				return scenEntry;
			}
		}
		return null;
	}
	
	
	public static void logDataEntry(String sessionId, String testCaseId, String tscenId, LinkedList<Map<String, Object>> metadataList) {
		for (Map<String, Object> metadata : metadataList) {
			logDataEntry(sessionId, testCaseId, tscenId, metadata, null, ReportManager.PASSED);
		}
	}
	
	public static void logDataEntry(String sessionId, String testCaseId, String tscenId, Map<String, Object> metadata) {
		logDataEntry(sessionId, testCaseId, tscenId, metadata, null, ReportManager.PASSED);
	}

	public static void logDataEntry(String sessionId, String testCaseId, String tscenId, LinkedList<Map<String, Object>> metadataList, String errorLog, String status) {
		for (Map<String, Object> metadata : metadataList) {
			logDataEntry(sessionId, testCaseId, tscenId, metadata, errorLog, status);
		}
	}
	
	public static void logDataEntry(String sessionId, String testCaseId, String tscenId,  Map<String, Object> metadata, String errorLog, String status) {
		DataEntry dataEntry = new DataEntry();
		dataEntry.addMetaData(metadata);
		dataEntry.setScenId(tscenId);
		dataEntry.setStatus(status);
		dataEntry.appendErrorLog(errorLog);
		dataEntry.setTestCaseId(testCaseId);
		logDataEntry(sessionId, dataEntry);
	}
	
	public static void logDataEntry(String sessionId, DataEntry dataEntry) {
		String scenId = dataEntryBySessionId.get(sessionId);
		if (scenId == null) {
			LinkedList<DataEntry> dataEntryList = dataEntries.get(dataEntry.getScenId());
			if (dataEntryList == null)  dataEntryList = new LinkedList<DataEntry>();
			dataEntryList.add(dataEntry);
			
			dataEntries.put(dataEntry.getScenId(), dataEntryList);
			dataEntryBySessionId.put(sessionId, dataEntry.getScenId());
		} else {
			for (DataEntry data : dataEntries.get(dataEntry.getScenId())) {
				if (data.equals(dataEntry)) {
					data.setStatus(dataEntry.getStatus());
					data.appendErrorLog(dataEntry.getErrorLog());
					if (data.checkMetaData(dataEntry.getMetaData().get(0))) {
						data.addAllMetaData(dataEntry.getMetaData());
					}
					break;
				}
			}
		}
	}
	
	public static void logImageEntry(String testCaseId, String scenId, String filePath, String status) {
		ImageEntry imageEntry = new ImageEntry();
		imageEntry.setTscenId(scenId);
		imageEntry.setTestCaseId(testCaseId);
		imageEntry.setImgFile(filePath);
		imageEntry.setStatus(status);
		logImageEntry(imageEntry);
		
	}
	public static void logImageEntry(ImageEntry imageEntry) {
		LinkedList<ImageEntry> imageEntryTemp = imageEntries.get(imageEntry.getTscenId());
		if (imageEntryTemp == null) imageEntryTemp = new LinkedList<ImageEntry>();
		imageEntryTemp.add(imageEntry);
		imageEntries.put(imageEntry.getTscenId(), imageEntryTemp);
	}
	

	public static LinkedList<TestCaseEntry> getTestCaseEntries() {
		return testCaseEntries;
	}
	
	public static LinkedList<ScenEntry> getScenEntries(String testCaseId) {
		return scenEntries.get(testCaseId);
	}
	
	public static LinkedList<DataEntry> getDataEntries(String tscenId) {
		return dataEntries.get(tscenId);
	}
	
	
	public static LinkedList<ImageEntry> getImageEntries(String tscenId) {
		return imageEntries.get(tscenId);
	}
	
	public static void completeTestCase(String testCaseId) {
		boolean testCaseFailed = false;
		int numfailed = 0;
		LinkedList<ScenEntry> scenEntryList = scenEntries.get(testCaseId);
		if (scenEntryList != null) {
			for (ScenEntry scen : scenEntries.get(testCaseId)) {
				if (scen.getStatus().equals(ReportManager.FAILED)
						|| scen.getStatus().equals(ReportManager.HALTED)) {
					numfailed++;
					testCaseFailed = true;
				}
			}			
		}
		
		TestCaseEntry testCase = getTestCaseEntry(testCaseId);
		testCase.setNumOfFailed(numfailed);
		if (testCaseFailed)
			testCase.setStatus(ReportManager.FAILED);
		else
			testCase.setStatus(ReportManager.PASSED);
	}
	
	public static void completeScen(String tscenId) {
		ScenEntry scen = scenEntriesByTscenId.get(tscenId);
		if (scen != null) {
			if (scen.getStatus().equals(ReportManager.INPROGRESS)) {
				boolean scenFailed = false;
				if (dataEntries.get(tscenId) != null) {
					for (DataEntry dataEntry : dataEntries.get(tscenId)) {
						if (dataEntry.getStatus().equals(ReportManager.FAILED)
								||dataEntry.getStatus().equals(ReportManager.HALTED)) {
							scenFailed=true;
							break;
						}
					}					
				}
				
				if (imageEntries.get(tscenId) != null) {
					for (ImageEntry imageEntry : imageEntries.get(tscenId)) {
						if (imageEntry.getStatus().equals(ReportManager.FAILED)
								|| imageEntry.getStatus().equals(ReportManager.HALTED)) {
							scenFailed=true;
							break;
						}
					}					
				}
				
				if (scenFailed) {
					scen.setStatus(ReportManager.FAILED);
				} else {
					scen.setStatus(ReportManager.PASSED);
				}			
			}
		}
	}
	
	public static void scenHalted(String testCaseId, String tscenId, String errorLog) {
		ScenEntry scen = scenEntriesByTscenId.get(tscenId);
		scen.appendErrorLog(errorLog);
		scen.setFailedRow(scen.getNumOfData());
		scen.setStatus(ReportManager.HALTED);
		
		LinkedList<DataEntry> dataEntryList = dataEntries.get(tscenId);
		if (dataEntryList != null) {
			for (DataEntry dataEntry : dataEntryList) {
				dataEntry.setStatus(ReportManager.FAILED);
			}
		}
		
		LinkedList<ImageEntry> imageEntryList = imageEntries.get(tscenId);
		if (imageEntryList != null) {
			for (ImageEntry imageEntry : imageEntryList) {
				imageEntry.setStatus(ReportManager.FAILED);
			}
		}
	}
	
	public static void testCaseHalted(String testCaseId, String errorLog) {
		boolean setErrorLog = false;
		int numfailed = 0;
		LinkedList<ScenEntry> scenEntryList = scenEntries.get(testCaseId);
		if (scenEntryList != null) {
			for (ScenEntry scenEntry : scenEntries.get(testCaseId)) {
				if (scenEntry.getStatus().equals(ReportManager.FAILED)) numfailed++;
				if (scenEntry.getStatus().equals(ReportManager.INPROGRESS)) {
					scenEntry.setStatus(ReportManager.HALTED);
					if (!setErrorLog)
						scenEntry.setErrorLog(errorLog);
					scenEntry.setFailedRow(scenEntry.getNumOfData());
					setErrorLog = true;
					numfailed++;
				}
			}
		}
		
		TestCaseEntry testCase = getTestCaseEntry(testCaseId);
		testCase.setStatus(ReportManager.HALTED);
		testCase.setNumOfFailed(numfailed);
	}
	
	public static void main(String[] args) {
		TestCaseEntry t =new TestCaseEntry();
		t.setTestCaseId("T1");
		
		ScenEntry s = new ScenEntry();
		s.setTscanId("S1");
		
		LinkedList<ScenEntry> en = new LinkedList<ScenEntry>();
		en.add(s);
		
		ReportMonitor.addTestCaseEntry(t, en);
		
		ReportMonitor.scenEntriesByTscenId.get("S1").setErrorLog("ERROR");
		System.out.println(ReportMonitor.scenEntries.get("T1").get(0).getErrorLog());
	}
	
	
}
