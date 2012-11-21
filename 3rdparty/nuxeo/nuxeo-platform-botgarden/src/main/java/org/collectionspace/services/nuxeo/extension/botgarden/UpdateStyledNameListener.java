package org.collectionspace.services.nuxeo.extension.botgarden;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.collectionspace.services.batch.nuxeo.FormatVoucherNameBatchJob;
import org.collectionspace.services.common.ResourceMap;
import org.collectionspace.services.loanout.nuxeo.LoanoutConstants;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class UpdateStyledNameListener implements EventListener {
	public static final String RUN_AFTER_MODIFIED_PROPERTY = "UpdateStyledNameListener.RUN_AFTER_MODIFIED";

	final Log logger = LogFactory.getLog(UpdateStyledNameListener.class);

	public void handleEvent(Event event) throws ClientException {
		EventContext ec = event.getContext();

		if (ec instanceof DocumentEventContext) {
			DocumentEventContext context = (DocumentEventContext) ec;
			DocumentModel doc = context.getSourceDocument();

			logger.debug("docType=" + doc.getType());
			
			if (doc.getType().startsWith(LoanoutConstants.NUXEO_DOCTYPE) && 
					!doc.isVersion() && 
					!doc.isProxy() && 
					!doc.getCurrentLifeCycleState().equals(LoanoutConstants.DELETED_STATE)) {
				
				if (event.getName().equals(DocumentEventTypes.BEFORE_DOC_UPDATE)) {
					DocumentModel previousDoc = (DocumentModel) context.getProperty(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL);	            	
	
					String previousLabelRequested = (String) previousDoc.getProperty(LoanoutConstants.LABEL_REQUESTED_SCHEMA_NAME, LoanoutConstants.LABEL_REQUESTED_FIELD_NAME);
					String labelRequested = (String) doc.getProperty(LoanoutConstants.LABEL_REQUESTED_SCHEMA_NAME, LoanoutConstants.LABEL_REQUESTED_FIELD_NAME);
					
					logger.debug("previousLabelRequested=" + previousLabelRequested + " labelRequested=" + labelRequested);
					
					if ((previousLabelRequested == null || previousLabelRequested.equals(LoanoutConstants.LABEL_REQUESTED_NO_VALUE)) && 
							labelRequested.equals(LoanoutConstants.LABEL_REQUESTED_YES_VALUE)) {
						// The label request is changing from no to yes, so we should update the styled name.
						ec.setProperty(RUN_AFTER_MODIFIED_PROPERTY, true);
					}
				}
				else {
					boolean doUpdate = false;
					
					if (event.getName().equals(DocumentEventTypes.DOCUMENT_CREATED)) {
						String labelRequested = (String) doc.getProperty(LoanoutConstants.LABEL_REQUESTED_SCHEMA_NAME, LoanoutConstants.LABEL_REQUESTED_FIELD_NAME);
						
						doUpdate = (labelRequested != null && labelRequested.equals(LoanoutConstants.LABEL_REQUESTED_YES_VALUE));
					}
					else {
						doUpdate = ec.hasProperty(RUN_AFTER_MODIFIED_PROPERTY) && ((Boolean) ec.getProperty(RUN_AFTER_MODIFIED_PROPERTY));
					}
					
					if (doUpdate) {
						logger.debug("Updating styled name");
	
						String voucherCsid = doc.getName();
						
						try {
							createFormatter().formatVoucherName(voucherCsid);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
			}
		}
	}
	
	private FormatVoucherNameBatchJob createFormatter() {
		ResourceMap resourceMap = ResteasyProviderFactory.getContextData(ResourceMap.class);

		FormatVoucherNameBatchJob formatter = new FormatVoucherNameBatchJob();
		formatter.setResourceMap(resourceMap);

		return formatter;
	}
}