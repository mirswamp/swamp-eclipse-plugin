package org.continuousassurance.swamp.eclipse;

/**
 * AssessmentDetails class is a grouper for information about a submitted assessment
 * @author reid-jr
 *
 */

public class AssessmentDetails {
	/**
	 * UUID of SWAMP project that assessment is for
	 */
	private String prjUUID;
	/**
	 * UUID of SWAMP assessment UUID
	 */
	private String assessUUID;
	/**
	 * Name of SWAMP package
	 */
	private String packageName;
	/**
	 * Name of package version
	 */
	private String packageVersion;
	/**
	 * Name of Eclipse project corresponding to SWAMP package
	 */
	private String eclipseProject;
	/**
	 * Time that assessment was submitted
	 */
	private String submissionTime;
	/**
	 * Most recent status
	 */
	private String status;
	/**
	 * DELIMITER for csv
	 */
	public static final String DELIMITER = ",";
	
	/**
	 * Constructor for AssessmentDetails object
	 * @param prjUUID UUID of the SWAMP project
	 * @param assessUUID UUID of the assessment
	 * @param packageName name of the package
	 * @param packageVersion version of the package
	 * @param eclipseProject name of Eclipse project
	 */
	public AssessmentDetails(String prjUUID, String packageName, String packageVersion, String eclipseProject) {
		this.prjUUID = prjUUID;
		this.packageName = packageName;
		this.packageVersion = packageVersion;
		this.eclipseProject = eclipseProject;
	}
	
	/**
	 * Setter for assessment UUID
	 * @param assessUUID assessment UUID
	 */
	public void setAssessmentUUID(String assessUUID) {
		this.assessUUID = assessUUID;
	}
	
	/**
	 * Setter for submission time
	 */
	public void setSubmissionTime() {
		submissionTime = Utils.getCurrentTimestamp();
	}
	
	/**
	 * Setter for status
	 * @param status status given by SwampApiWrapper
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
	/**
	 * Serializes the AssessmentDetail
	 * @return String with the info for the assessment
	 */
	public String serialize() {
		String str = prjUUID + DELIMITER + assessUUID + DELIMITER + packageName + 
				DELIMITER + packageVersion + DELIMITER + eclipseProject + DELIMITER + submissionTime + 
				DELIMITER + status + "\n";
		return str;
	}
	
	/**
	 * Overriden toString()
	 */
	@Override
	public String toString() {
		return serialize();
	}
	
	/**
	 * Takes a serialized AssessmentDetail String and replaces old status with new status
	 * @param serializedAssessmentDetails serialized AssessmentDetail
	 * @param status new status
	 * @return updated serialized AssessmentDetail
	 */
	public static String updateStatus(String serializedAssessmentDetails, String status) {
		String[] parts = serializedAssessmentDetails.split(DELIMITER);
		parts[6] = status;
		StringBuilder sb = new StringBuilder(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			sb.append(DELIMITER);
			sb.append(parts[i]);
		}
		sb.append("\n");
		return sb.toString();
	}
}
