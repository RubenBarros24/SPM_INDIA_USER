
package com.sap.model;

import com.sap.requestTypes.QueryParams;

public class PropertiesData {

	/**
	 * this variable stores the personIdExternal
	 */
	private String extId;

	/**
	 * this variable represents the value of the key firstName
	 */
	private String firstNameNodeValue;
	/**
	 * this variable represents the value of the key lastName
	 */
	private String lastNameNodeValue;
	/**
	 * this variable represents the value of the key dateOfBirth
	 */
	private String dateOfBirthNodeValue;
	/**
	 * this variable represents the value of the key countryOfBirth
	 */
	private String countryOfBirthNodeValue;

	/**
	 * default constructor
	 */
	public PropertiesData() {

	}

	/**
	 * constructor
	 * 
	 * @param id
	 */
	public PropertiesData(String id, String type) {
				
		switch(type){
		
		case "PerPersonal":
			this.extId = "personIdExternal%20eq%20'" + id + "'";
		break;
		
		case "PayComp":
			this.extId = "userId%20eq%20'" + id + "'";
		break;
		
		case "EmpJob":
			this.extId = "userId%20eq%20'" + id + "'";
		break;
		
		case "EmpEmployment":
			this.extId = "userId%20eq%20'" + id + "'";
		break;
		
		case "EmpCompensation":
			this.extId = "userId%20eq%20'" + id + "'";
		break;
		
		case "PerPerson":
			this.extId = "personIdExternal%20eq%20'" + id + "'";
		break;
		
		case "PerEmail":
			this.extId = "personIdExternal%20eq%20'" + id + "'";
		break;
			
		default: break;
		}
	}

	/**
	 * this method return the personIdExternal
	 * 
	 * @return
	 */
	public String getExtId() {
		return this.extId;
	}
	
	public String getPerPersonalDest() {
		return QueryParams.SERVICEURL + QueryParams.PERPERSONAL + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("PerPersonal");
	}
	
	public String getPayCompDest() {
		return QueryParams.SERVICEURL + QueryParams.EMPPAYCOMPRECURRING + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("PayComp");
	}
	
	public String getEmpJobDest() {
		return QueryParams.SERVICEURL + QueryParams.EMPJOB + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("EmpJob");
	}

	public String getEmpEmploymentDest() {
		return QueryParams.SERVICEURL + QueryParams.EMPEMPLOYMENT + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("EmpEmployment");
	}

	public String getEmpCompensationDest() {
		return QueryParams.SERVICEURL + QueryParams.EMPCOMPENSATION + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("EmpCompensation");
	}

	public String getPerPersonDest() {
		return QueryParams.SERVICEURL + QueryParams.PERPERSON + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("PerPerson");
	}

	public String getPerEmailDest() {
		return QueryParams.SERVICEURL + QueryParams.PEREMAIL + "?" + "$filter=" + this.getExtId()
		+ this.getURLParam("PerEmail");
	}
	
	public String getURLParam(String type) {
		
		String URLPARAM = "";	
		
		switch(type){
		
			case "PerPersonal":
				
			URLPARAM = "&$expand=" + QueryParams.PERSONNAV + "&$select=" + QueryParams.FIRSTNAME + ","
					+ QueryParams.LASTNAME + "," + QueryParams.GENDER + ","  
					+ QueryParams.PERSONNAV + "/" + QueryParams.DATEOFBIRTH;
				
			break;
			
			case "PayComp":
				
			URLPARAM = "&$select=" + QueryParams.PAYCOMPONENT + ","
					+ QueryParams.PAYCOMPVALUE + "," + QueryParams.CURRENCYCODE + ","
					+ QueryParams.FREQUENCY + "," + QueryParams.CUSTOMDOUBLE1 + QueryParams.FILTERPAYCOMP;
				
			break;
			
			case "EmpJob":
				
			URLPARAM = "&$select=" + QueryParams.COMPANY + ","
						+ QueryParams.LOCATION + "," + QueryParams.EMPLOYEECLASS + ","
						+ QueryParams.EMPLOYMENTTYPE + "," + QueryParams.FTE + ","
						+ QueryParams.PROBATIONPERIODENDDATE + "," + QueryParams.ISFULLTIMEEMPLOYEE + ","
						+ QueryParams.JOBENTRYDATE + "," + QueryParams.CUSTOMSTRING7 + ","
						+ QueryParams.CUSTOMSTRING31 + "," + QueryParams.CUSTOMSTRING49 + ","
						+ QueryParams.JOBTITLE;
					
			break;
				
			case "EmpEmployment":
				
			URLPARAM = "&$select=" + QueryParams.ORIGINALSTARTDATE + ","
						+ QueryParams.STARTDATE + "," + QueryParams.CUSTOMDATE1 + "," 
						+ QueryParams.ENDDATE;
					
			break;
				
			case "EmpCompensation":
				
			URLPARAM = "&$select=" + QueryParams.PAYGROUP + "," + QueryParams.STARTDATE;
					
			break;
				
			case "PerPerson":
				
			URLPARAM = "&$expand=" + QueryParams.HOMEADDRESSNAVDEFLT 
						+ "&$select=" + QueryParams.PERSONIDEXTERNAL + "," + QueryParams.CUSTOMSTRING1 + "," 
						+ QueryParams.CUSTOMSTRING2;
					
			break;
				
			case "PerEmail":
				
			URLPARAM = "&$select=" + QueryParams.EMAILTYPE + ","
						+ QueryParams.EMAILADDRESS;
					
			break;
		
		default: break;
		
		}
		
		return URLPARAM;
	}

	/**
	 * this method returns the value of the given key
	 * 
	 * @param key
	 * @return
	 */
	public String getNodeValue(String key) {
		if (key == QueryParams.FIRSTNAME) {
			return this.firstNameNodeValue;
		} else if (key == QueryParams.LASTNAME) {
			return this.lastNameNodeValue;
		} else if (key == QueryParams.DATEOFBIRTH) {
			return this.dateOfBirthNodeValue;
		} else if (key == QueryParams.COUNTRYOFBIRTH) {
			return this.countryOfBirthNodeValue;
		} else {
			System.out.println("unhandled key");
			return "";
		}
	}

	/**
	 * this method sets the personIdExternal
	 * 
	 * @param id
	 */
	public void setExtId(String id) {
		this.extId = "personIdExternal eq '" + id + "'";
	}

	/**
	 * this method sets the value of the key
	 * 
	 * @param key
	 * @param nodeValue
	 */
	public void setNodeValue(String key, String nodeValue) {
		if (key == QueryParams.FIRSTNAME) {
			this.firstNameNodeValue = nodeValue;
		} else if (key == QueryParams.LASTNAME) {
			this.lastNameNodeValue = nodeValue;
		} else if (key == QueryParams.DATEOFBIRTH) {
			this.dateOfBirthNodeValue = nodeValue;
		} else if (key == QueryParams.COUNTRYOFBIRTH) {
			this.countryOfBirthNodeValue = nodeValue;
		} else {

		}
	}
}
