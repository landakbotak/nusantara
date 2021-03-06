package com.nusantara.automate.function;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.nusantara.automate.Actionable;
import com.nusantara.automate.Assertion;
import com.nusantara.automate.DBConnection;
import com.nusantara.automate.Statement;
import com.nusantara.automate.WebExchange;
import com.nusantara.automate.exception.FailedTransactionException;
import com.nusantara.automate.query.QueryEntry;
import com.nusantara.automate.report.ReportManager;
import com.nusantara.automate.report.ReportMonitor;
import com.nusantara.automate.report.SnapshotEntry;
import com.nusantara.automate.util.DataTypeUtils;
import com.nusantara.automate.util.MapUtils;
import com.nusantara.automate.util.StringUtils;

public class AssertQueryAction implements Actionable {

	Logger log = LoggerFactory.getLogger(AssertQueryAction.class);
	
	@Value("active_scen")
	private String testcase;
	
	@Value("active_workflow")
	private String scen;
	
	private QueryEntry qe;
	
	public AssertQueryAction(QueryEntry qe) {
		this.qe = qe;
	}
	
	@Override
	public void submit(WebExchange webExchange) throws FailedTransactionException {
		log.info("Query -> " + qe.getQuery());
	
		boolean executeBatch = false;
		for (String param : qe.getParameters()) {
			if (param.startsWith("@" + WebExchange.PREFIX_TYPE_DATA))
				executeBatch = true;
		}
		
		if (executeBatch) {
			for (int i=0; i<webExchange.getCountSession(); i++) {
				try {
					webExchange.setCurrentSession(i);
					assertQuery(webExchange);
				} catch (FailedTransactionException e) {
					webExchange.addFailedSession(webExchange.getCurrentSession());
					ReportMonitor.logError(webExchange.get("active_scen").toString(),
							webExchange.get("active_workflow").toString(), e.getMessage());
				}
			}
		} else {
			try {
				assertQuery(webExchange);
			} catch (FailedTransactionException e) {
				webExchange.addFailedSession(webExchange.getCurrentSession());
				ReportMonitor.logError(webExchange.get("active_scen").toString(),
						webExchange.get("active_workflow").toString(), e.getMessage());
			}
		}
	}
	
	private void assertQuery(WebExchange webExchange) throws FailedTransactionException {
		String[] columns = new String[qe.getStatements().size()];
		columns = qe.getColumns().toArray(columns);
		
		List<String[]> result = new ArrayList<String[]>();
		List<Assertion> asserts = new LinkedList<Assertion>();
		try {
			int i = 0;
			for (String query : qe.getParsedQuery(webExchange)) {

				log.info("Execute Query -> " + query);
				result = DBConnection.selectSimpleQuery(query, columns);
			
				if (result.size() ==0)
					result.add(new String[columns.length]);
					
				Assertion assertion = new Assertion();
				assertion.setQuery(query);
				assertion.setResult(StringUtils.asStringTableHtml(columns, result));
				for (String[] res : result) {
					Map<String, String> resultMap = MapUtils.copyAsMap(columns, res, String.class, String.class);
					for (Statement state : qe.getStatements(i).values()) {
						Statement statement = new Statement(state);
						if (statement.getEquality() != null) {
							if (statement.isArg1(DataTypeUtils.TYPE_OF_COLUMN)) {
								statement.setVal1(resultMap.get(statement.getArg1()));
							} else if (statement.isArg1(DataTypeUtils.TYPE_OF_VARIABLE)) {
								statement.setVal1(StringUtils.nvl(webExchange.get(statement.getArg1()),"null"));
							} else {
								statement.setVal1(statement.getArg1());
							}
							if (statement.isArg2(DataTypeUtils.TYPE_OF_COLUMN)) {
								statement.setVal2(resultMap.get(statement.getArg2()));
							} else if (statement.isArg2(DataTypeUtils.TYPE_OF_VARIABLE)) {
								statement.setVal2(StringUtils.nvl(webExchange.get(statement.getArg2()),"null"));
							} else {
								statement.setVal2(statement.getArg2());
							}
								
							assertion.addStatement(statement);
						}
					}
					i++;
				}
				asserts.add(assertion);
			}
		} catch (Exception e) {
			log.error("Failed execute query ", e);
			throw new FailedTransactionException(e.getMessage());
		}
		
		String rawText = "";
		boolean status = true;
		for (Assertion e : asserts) {
			if (!rawText.isEmpty())
				rawText += "<br><br>";
			rawText += e.getAssertion();
			if (status) status = e.isTrue();
		}
		
		SnapshotEntry entry = new SnapshotEntry();
		entry.setTscenId(scen);
		entry.setTestCaseId(testcase);
		entry.setSnapshotAs(SnapshotEntry.SNAPSHOT_AS_RAWTEXT);
		entry.setRawText(rawText);
		entry.setStatus((status ? ReportManager.PASSED : ReportManager.FAILED));
		
		ReportMonitor.logSnapshotEntry(entry);
		
		if (!status)
			throw new FailedTransactionException("Failed assertion", entry);
	}
}
