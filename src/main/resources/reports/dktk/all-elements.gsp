<%@ page import="de.samply.reporter.context.Context" %>
<% Context dataModel = context %>
<%
    def patientsProAttributeValueKey = "Patients pro attribute-value"
    def patientsProAttributeKey = "Patients pro attribute"
    def validationKey = "Validation"
    def emptyValue = ""
    def DELIMITER = dataModel.getCsvConfig().delimiter()
    def totalNumberOfPatients = dataModel.getElement("total number of patients")
%>
<% dataModel.getKeySet(patientsProAttributeKey).forEach { attribute ->
    def patientsForAttribute = ((Set) dataModel.getElement(patientsProAttributeKey, attribute))
    def numberOfPatientsForAttribute = (patientsForAttribute != null) ? patientsForAttribute.size() : 0
    def isEmptyValueToBeIgnored = dataModel.getKeySet(patientsProAttributeValueKey, attribute).size() > 1
    dataModel.getKeySet(patientsProAttributeValueKey, attribute).forEach { value ->
        if (!value.equals(emptyValue) || !isEmptyValueToBeIgnored) {
            def patientsForAttributeValue = (value.trim().equals(emptyValue)) ? [] : ((Set) dataModel.getElement(patientsProAttributeValueKey, attribute, value))
            def numberOfPatientsForAttributeValue = (patientsForAttributeValue != null) ? patientsForAttributeValue.size() : 0
            def matchValue = (dataModel.getElement(validationKey, attribute, value) == null) ? "match" : "mismatch"
            def lineElements = [
                    attribute,
                    value,
                    (numberOfPatientsForAttributeValue > 0) ? matchValue : emptyValue,
                    numberOfPatientsForAttributeValue,
                    (numberOfPatientsForAttribute != 0) ? (100.0 * numberOfPatientsForAttributeValue / numberOfPatientsForAttribute).round(1) : 0,
                    (totalNumberOfPatients != 0) ? (100.0 * numberOfPatientsForAttributeValue / totalNumberOfPatients).round(1) : 0
            ]
            if (lineElements[0] != null && lineElements[0].trim().size() > 0) {
%>
${lineElements.join(DELIMITER)}
<% }
}
}
} %>
