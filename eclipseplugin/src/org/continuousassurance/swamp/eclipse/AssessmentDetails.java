package org.continuousassurance.swamp.eclipse;

/**
 * AssessmentDetails class is a grouper for information about a submitted assessment
 * @author reid-jr
 *
 */

public class AssessmentDetails {
	
	/**
	 * String file path where the results should be stored
	 */
	private String resultsFilepath;
	
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
	 * Tool assessment was run on
	 */
	private String tool;
	
	/**
	 * Platform assessment was submitted on
	 */
	private String platform;
	
	/**
	 * Number of bugs found by assessment
	 */
	private String bugCount;
	
	/**
	 * DELIMITER for csv
	 */
	public static final String DELIMITER = ",";
	
	/**
	 * Number of hidden fields in serialized assessment details
	 */
	public static final int NUM_HIDDEN_FIELDS = 3;
	
	public static final int ASSESS_UUID_PART = 2;
	
	public static final int RESULTS_FILEPATH_PART = 0;
	
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
		this.resultsFilepath = "";
		this.assessUUID = "";
		this.submissionTime = "";
		this.status = "";
		this.tool = "";
		this.platform = "";
		this.bugCount = "";
	}
	
	public AssessmentDetails(String serializedDetails) {
		String[] parts = serializedDetails.split(DELIMITER);
		this.resultsFilepath = parts[0];
		this.prjUUID = parts[1];
		this.assessUUID = parts[2];
		this.packageName = parts[3];
		this.packageVersion = parts[4];
		this.tool = parts[5];
		this.submissionTime = parts[6];
		this.status = parts[7];
		this.bugCount = parts[8];
		this.eclipseProject = parts[9];
		this.platform = parts[10];
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
	
	public void setResultsFilepath(String filepath) {
		this.resultsFilepath = filepath;
	}
	
	/**
	 * Serializes the AssessmentDetail
	 * @return String with the info for the assessment
	 */
	public String serialize() {
		String str = resultsFilepath + DELIMITER + prjUUID + DELIMITER + assessUUID + DELIMITER + packageName + 
				DELIMITER + packageVersion + DELIMITER + tool + DELIMITER + submissionTime + 
				DELIMITER + status + DELIMITER + bugCount + DELIMITER + eclipseProject + DELIMITER + 
				platform + DELIMITER + "\n";
		return str;
	}
	
	/**
	 * Overriden toString()
	 */
	@Override
	public String toString() {
		return serialize();
	}
	
	public void setToolName(String toolName) {
		tool = toolName;
	}
	
	public void setPlatformName(String platformName) {
		platform = platformName;
	}
	
	public String getProjectUUID() {
		return prjUUID;
	}
	
	public String getAssessUUID() {
		return assessUUID;
	}
	
	/**
	 * Takes a serialized AssessmentDetail String and replaces old status with new status
	 * @param serializedAssessmentDetails serialized AssessmentDetail
	 * @param status new status
	 * @return updated serialized AssessmentDetail
	 */
	public void updateStatus(String status) {
		this.status = status;
	}
	
	public void setBugCount(int bugCount) {
		this.bugCount = Integer.toString(bugCount);
	}
	
	public String getFilepath() {
		return this.resultsFilepath;
	}
}