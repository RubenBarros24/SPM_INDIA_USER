package servlets;

/**
 * Ruben Barros (C5249059)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.sap.security.auth.login.LoginContextFactory;
import com.sap.requestTypes.HttpRequest;
import com.sap.requestTypes.HttpPostRequest;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import static java.net.HttpURLConnection.HTTP_OK;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.core.connectivity.api.http.HttpDestination;
import com.sap.model.JSONArray;
import com.sap.model.JSONObject;
import com.sap.core.connectivity.api.DestinationFactory;

/**
 * Servlet implementation class StartThread
 */
@WebServlet("/")
public class StartThread extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final int COPY_CONTENT_BUFFER_SIZE = 1024;
	private static final Logger LOGGER = LoggerFactory.getLogger(StartThread.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StartThread() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String myid = request.getParameter("userID");


		if (myid == null) {

			doPost(request, response);

		} else if (myid.equals("processLogin")) {

			String user = request.getRemoteUser();
			if (user != null) {

				String output = "Hello, " + request.getRemoteUser();
				response.getOutputStream().write(output.getBytes());

			} else {
				LoginContext loginContext;
				try {
					loginContext = LoginContextFactory.createLoginContext("FORM");
					loginContext.login();

					String output = "Hello, " + request.getRemoteUser();
					response.getOutputStream().write(output.getBytes());

				} catch (LoginException e) {
					e.printStackTrace();
				}

			}

		} else {

			try {
				String output = "";
				String id = "";
				String ext_id = "";
				String user = request.getRemoteUser();
				if (user != null) {

					HttpClient httpClient = null;
					String destinationName = "int_pg";

					try {
						// Get HTTP destination
						Context ctx = new InitialContext();
						HttpDestination destination = null;
						if (destinationName != null) {
							DestinationFactory destinationFactory = (DestinationFactory) ctx
									.lookup(DestinationFactory.JNDI_NAME);
							destination = (HttpDestination) destinationFactory.getDestination(destinationName);
						} else {
							destinationName = "int_pg";
							destination = (HttpDestination) ctx.lookup("java:comp/env/" + destinationName);
						}

						// Create HTTP client
						httpClient = destination.createHttpClient();

						// Execute HTTP request
						HttpGet httpGet = new HttpGet("/sap/ZHR_SALARY_PACKAGING_HCP_SRV/ZT5W7ASet?$format=json");
						HttpResponse httpResponse = httpClient.execute(httpGet);

						// Check response status code
						int statusCode = httpResponse.getStatusLine().getStatusCode();
						if (statusCode != HTTP_OK) {
							throw new ServletException(
									"Expected response status code is 200 but it is " + statusCode + " .");
						}

						// Copy content from the incoming response to the
						// outgoing
						// response
						HttpEntity entity = httpResponse.getEntity();
						if (entity != null) {
							InputStream instream = entity.getContent();
							try {
								byte[] buffer = new byte[COPY_CONTENT_BUFFER_SIZE];
								int len;
								String callBackJavaScripMethodName = request.getParameter("callback") + "(";
								String callBackJavaScripEnd = ");";
								response.setContentType("text/javascript");

								response.getOutputStream().write(callBackJavaScripMethodName.getBytes());

								String responseString = EntityUtils.toString(entity, "UTF-8");

								JSONObject allowancesJSON = new JSONObject(responseString);
								JSONObject d = allowancesJSON.getJSONObject("d");
								JSONArray results = d.getJSONArray("results");

								for (int i = 0; i < results.length(); i++) {
									JSONObject obj = results.getJSONObject(i);
									id = obj.getString("UserId");
									ext_id = obj.getString("F4hfm");

								}

								String[] typesArray = new String[] { "PerPerson", "PayComp", "EmpJob", "EmpEmployment",
										"EmpCompensation", "PerPersonal", "PerEmail" };
								// String[] typesArray = new
								// String[]{"PerPerson"};

								Thread myThreads[] = new Thread[typesArray.length];
								System.out.println("Thread started....");

								// response.getWriter().append("Thread
								// started....");
								if (id != null) {

									for (int i = 0; i < typesArray.length; i++) {

										if (typesArray[i].equals("PerPersonal") || typesArray[i].equals("PerEmail")) {
											myThreads[i] = new HttpRequest(ext_id, typesArray[i]);
										} else {
											myThreads[i] = new HttpRequest(id, typesArray[i]);
										}
										myThreads[i].start();
									}
									for (int j = 0; j < typesArray.length; j++) {

										try {
											myThreads[j].join();
										} catch (InterruptedException ie) {
										}

										String result = ((HttpRequest) myThreads[j]).getResult();

										if (result != null) {

											if (j == 0) {

												output = output + "{" + '"' + "root" + '"' + ":";
												output = output + result.substring(0, result.length() - 1)
														.replace("feed", typesArray[j]);

											} else {

												output = output + result.substring(1, result.length() - 1)
														.replace("feed", typesArray[j]);
											}

											if (j < typesArray.length - 1) {

												output = output + ",";

											} else if (j == typesArray.length - 1) {

												output = output + "}";
												output = output + "}";

											}
										}
									}

									output = output.replaceFirst(",", "{");
									response.getOutputStream().write(output.getBytes());
									// response.getWriter().append(output);
									response.getOutputStream().write(callBackJavaScripEnd.getBytes());
								}

							} catch (IOException e) {
								// In case of an IOException the connection will
								// be
								// released
								// back to the connection manager automatically
								throw e;
							} catch (RuntimeException e) {
								// In case of an unexpected exception you may
								// want
								// to abort
								// the HTTP request in order to shut down the
								// underlying
								// connection immediately.
								httpGet.abort();
								throw e;
							} finally {
								// Closing the input stream will trigger
								// connection
								// release
								try {
									instream.close();
								} catch (Exception e) {
									// Ignore
								}
							}
						}
					} catch (NamingException e) {
						// Lookup of destination failed
						String errorMessage = "Lookup of destination failed with reason: " + e.getMessage() + ". See "
								+ "logs for details. Hint: Make sure to have the destination " + destinationName
								+ " configured.";
						LOGGER.error("Lookup of destination failed", e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
					} catch (Exception e) {
						// Connectivity operation failed
						String errorMessage = "Connectivity operation failed with reason: " + e.getMessage() + ". See "
								+ "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
								+ "local Eclipse environment in case your environment uses "
								+ "an HTTP proxy for the outbound Internet " + "communication.";
						LOGGER.error("Connectivity operation failed", e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
					} finally {
						// When HttpClient instance is no longer needed, shut
						// down
						// the connection manager to ensure immediate
						// deallocation of all system resources
						if (httpClient != null) {
							httpClient.getConnectionManager().shutdown();
						}
					}

				} else {
					LoginContext loginContext;
					try {
						loginContext = LoginContextFactory.createLoginContext("FORM");
						loginContext.login();

						// output = output + "Hello, " +
						// request.getRemoteUser();

						HttpClient httpClient = null;
						String destinationName = "int_pg";
						try {
							// Get HTTP destination
							Context ctx = new InitialContext();
							HttpDestination destination = null;
							if (destinationName != null) {
								DestinationFactory destinationFactory = (DestinationFactory) ctx
										.lookup(DestinationFactory.JNDI_NAME);
								destination = (HttpDestination) destinationFactory.getDestination("int_pg");
							} else {
								// The default request to the Servlet will use
								// outbound-internet-destination
								destinationName = "int_pg";
								destination = (HttpDestination) ctx.lookup("java:comp/env/" + destinationName);
							}

							// Create HTTP client
							httpClient = destination.createHttpClient();

							// Execute HTTP request
							HttpGet httpGet = new HttpGet("/sap/ZHR_SALARY_PACKAGING_HCP_SRV/ZT5W7ASet?$format=json");
							HttpResponse httpResponse = httpClient.execute(httpGet);

							// Check response status code
							int statusCode = httpResponse.getStatusLine().getStatusCode();
							if (statusCode != HTTP_OK) {
								throw new ServletException(
										"Expected response status code is 200 but it is " + statusCode + " .");
							}

							// Copy content from the incoming response to the
							// outgoing response
							HttpEntity entity = httpResponse.getEntity();
							if (entity != null) {
								InputStream instream = entity.getContent();
								try {
									byte[] buffer = new byte[COPY_CONTENT_BUFFER_SIZE];
									int len;
									String callBackJavaScripMethodName = request.getParameter("callback") + "(";
									String callBackJavaScripEnd = ");";
									response.setContentType("text/javascript");

									response.getOutputStream().write(callBackJavaScripMethodName.getBytes());

									String responseString = EntityUtils.toString(entity, "UTF-8");

									response.getOutputStream().write(responseString.getBytes());
									
									response.getOutputStream().write(callBackJavaScripEnd.getBytes());
								} catch (IOException e) {
									// In case of an IOException the connection
									// will
									// be released
									// back to the connection manager
									// automatically
									throw e;
								} catch (RuntimeException e) {
									// In case of an unexpected exception you
									// may
									// want to abort
									// the HTTP request in order to shut down
									// the
									// underlying
									// connection immediately.
									httpGet.abort();
									throw e;
								} finally {
									// Closing the input stream will trigger
									// connection release
									try {
										instream.close();
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						} catch (NamingException e) {
							// Lookup of destination failed
							String errorMessage = "Lookup of destination failed with reason: " + e.getMessage()
									+ ". See " + "logs for details. Hint: Make sure to have the destination "
									+ destinationName + " configured.";
							LOGGER.error("Lookup of destination failed", e);
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
						} catch (Exception e) {
							// Connectivity operation failed
							String errorMessage = "Connectivity operation failed with reason: " + e.getMessage()
									+ ". See "
									+ "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
									+ "local Eclipse environment in case your environment uses "
									+ "an HTTP proxy for the outbound Internet " + "communication.";
							LOGGER.error("Connectivity operation failed", e);
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
						} finally {
							// When HttpClient instance is no longer needed,
							// shut
							// down the connection manager to ensure immediate
							// deallocation of all system resources
							if (httpClient != null) {
								httpClient.getConnectionManager().shutdown();
							}
						}

					} catch (LoginException e) {
						e.printStackTrace();
					}
				}

				System.out.println("Done GET");
				// response.getWriter().append("Done");
			} catch (Exception e) {
				response.getWriter().append(e.getMessage());
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String id = request.getParameter("userID");
		boolean errorFlag = false;
		boolean CarLeaseFound = false;
		boolean TransportLeaseFound = false;
		boolean writeMsg = true;
		Double HouseRentLimit;
		Double fte = 1.0;
		String startDate;
		System.out.println("Do Post Called1");
		String body = null;
		String message = null;
		String compensationMessage = null;

		try {

			body = request.getParameter("msg").toString();

		} catch (Exception e) {
		}
		System.out.println(body);
		if (id != null) {

			doGet(request, response);

		} else {

			try {

				String user = request.getRemoteUser();
				System.out.println("Do Post Called2");
				if (user != null) {

					HttpClient httpClient = null;
					String destinationName = "int_pg";
					System.out.println("Do Post Called");
					try {
						// Get HTTP destination
						Context ctx = new InitialContext();
						HttpDestination destination = null;
						if (destinationName != null) {
							DestinationFactory destinationFactory = (DestinationFactory) ctx
									.lookup(DestinationFactory.JNDI_NAME);
							destination = (HttpDestination) destinationFactory.getDestination("int_pg");
						} else {
							// The default request to the Servlet will use
							// outbound-internet-destination
							destinationName = "int_pg";
							destination = (HttpDestination) ctx.lookup("java:comp/env/" + destinationName);
						}

						// Create HTTP client
						httpClient = destination.createHttpClient();

						// Execute HTTP request
						HttpGet httpGet = new HttpGet("/sap/ZHR_SALARY_PACKAGING_HCP_SRV/ZT5W7ASet?$format=json");
						HttpResponse httpResponse = httpClient.execute(httpGet);

						HttpGet httpGetp0589 = new HttpGet("/sap/ZHR_SALARY_PACKAGING_HCP_SRV/p0589Set?$format=json");
						HttpResponse httpResponsep0589 = httpClient.execute(httpGetp0589);

						// Check response status code
						int statusCode = httpResponse.getStatusLine().getStatusCode();
						if (statusCode != HTTP_OK) {
							throw new ServletException(
									"Expected response status code is 200 but it is " + statusCode + " .");
						}

						// Copy content from the incoming response to the
						// outgoing
						// response
						HttpEntity entity = httpResponse.getEntity();
						HttpEntity entityp0589 = httpResponsep0589.getEntity();

						if (entity != null && body != null) {
							InputStream instream = entity.getContent();
							try {
								byte[] buffer = new byte[COPY_CONTENT_BUFFER_SIZE];
								int len;

								body = body.replaceAll("\\r\\n", "");

								System.out.println("Starting parse...");
								System.out.println(body);
								JSONArray allowancesJSON = new JSONArray(body);

								String responseString = EntityUtils.toString(entity, "UTF-8");

								JSONObject allowancesJSONIPP = new JSONObject(responseString);
								JSONObject d = allowancesJSONIPP.getJSONObject("d");
								JSONArray results = d.getJSONArray("results");

								String responseStringP0589 = EntityUtils.toString(entityp0589, "UTF-8");

								JSONObject p0589JSONIPP = new JSONObject(responseStringP0589);
								JSONObject p0589d = p0589JSONIPP.getJSONObject("d");
								JSONArray p0589results = p0589d.getJSONArray("results");
								System.out.println("Do Post p0589 after");
								String status = "Y";
								String existence = "Y";

								for (int x = 0; x < p0589results.length(); x++) {

									JSONObject objp0589IPP = p0589results.getJSONObject(x);

									status = objp0589IPP.getString("Flag1");
									existence = objp0589IPP.getString("Flag2");

									if (status == "Y") {

										errorFlag = true;
									}

								}

								if (errorFlag == false) {

									System.out.println("Before obj IPP");

									for (int i = 0; i < results.length(); i++) {

										JSONObject objIPP = results.getJSONObject(i);

										id = objIPP.getString("UserId");
										String Carea = objIPP.getString("Carea");
										String Lgart = objIPP.getString("Lgart");
										String MaxFg = objIPP.getString("Maxfg");
										String MinFg = objIPP.getString("Minfg");
										String Pkind = objIPP.getString("Pkind");
										String Metro = objIPP.getString("Area");

										if (id != null) {

											HttpRequest jobThread = new HttpRequest(id, "EmpJob");
											jobThread.start();

											try {

												jobThread.join();

											} catch (InterruptedException ie) {
											}

											String result = ((HttpRequest) jobThread).getResult();

											JSONObject jobAllowance = new JSONObject(result);
											JSONObject feed = jobAllowance.getJSONObject("feed");

											JSONObject entries = feed.getJSONObject("entry");

											JSONObject content = entries.getJSONObject("content");

											JSONObject properties = content.getJSONObject("m:properties");

											JSONObject fteContent = properties.getJSONObject("d:fte");

											fte = fteContent.getDouble("content");

											System.out.println("We found fte field:" + fte);

										}

										int year = Calendar.getInstance().get(Calendar.YEAR);
										int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
										int dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
										String month;
										String day;

										if (currentMonth < 10) {

											month = "0"
													+ String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);

										} else {

											month = String.valueOf(currentMonth);
										}

										if (dayOfMonth < 10) {

											day = "0"
													+ String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

										} else {

											day = String.valueOf(dayOfMonth);

										}

										if (existence == "Y") {

											/*
											 * User exists in previous SPM
											 */

											if (currentMonth == 4) {

												if (dayOfMonth < 21) {

													startDate = year + "/" + month + "/01";

												} else {

													currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

													if (currentMonth < 10) {

														month = "0" + String.valueOf(
																Calendar.getInstance().get(Calendar.MONTH) + 1);

													} else {

														month = String.valueOf(currentMonth);
													}

													if (currentMonth == 13) {

														month = "01";
														year++;
													}

													startDate = year + "/" + month + "/01";
												}

											} else {

												if (dayOfMonth < 16) {

													startDate = year + "/" + month + "/" + day;

												} else {

													currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

													if (currentMonth < 10) {

														month = "0" + String.valueOf(
																Calendar.getInstance().get(Calendar.MONTH) + 1);

													} else {

														month = String.valueOf(currentMonth);
													}

													if (currentMonth == 13) {

														month = "01";
														year++;
													}

													startDate = year + "/" + month + "/01";
												}

											}

										} else {

											/*
											 * User does not exist in previous
											 * SPM
											 */

											if (dayOfMonth < 16) {

												startDate = year + "/" + month + "/01";

											} else {

												currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

												if (currentMonth < 10) {

													month = "0" + String
															.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);

												} else {

													month = String.valueOf(currentMonth);
												}

												if (currentMonth == 13) {

													month = "01";
													year++;
												}

												startDate = year + "/" + month + "/01";
											}

										}

										System.out.println("After dates");
										System.out.println(allowancesJSON.toString());
										String startDateCompare = startDate.replaceAll("/", "-") + "T00:00:00";

										DateFormat formatter;
										formatter = new SimpleDateFormat("yyyy/MM/dd");

										Date date = formatter.parse(startDate);

										System.out.println(date.getTime() * 1000);
										Long l = new Long(date.getTime());
										startDate = "/Date(" + l.toString() + ")/";
										System.out.println("Startdate is : " + startDate);

										HttpRequest compensationThread = new HttpRequest(id, "EmpCompensation");
										compensationThread.start();

										try {

											compensationThread.join();

										} catch (InterruptedException ie) {
										}

										String resultComp = ((HttpRequest) compensationThread).getResult();

										System.out.println("Compensation result is:" + resultComp.toString());
										JSONObject compensationAllowance = new JSONObject(resultComp);
										JSONObject compensationFeed = compensationAllowance.getJSONObject("feed");

										JSONObject compensationEntry = compensationFeed.getJSONObject("entry");

										JSONObject compensationInnerContent = compensationEntry
												.getJSONObject("content");

										JSONObject compensationProperties = compensationInnerContent
												.getJSONObject("m:properties");

										String compensationStartDate = compensationProperties
												.getJSONObject("d:startDate").getString("content");

										System.out.println("Compensation date is: " + compensationStartDate
												+ " and cutoffDate is: " + startDateCompare);

										if (!compensationStartDate.equals(startDateCompare)) {

											System.out.println("Updating Employee compensation row...");

											String metaPutComp = "EmpCompensation(userId='" + id
													+ "' , startDate=datetime'" + compensationStartDate + "')";
											metaPutComp = metaPutComp.replaceAll("\"", "\\'");
											String eventReason = "Z_D_MIGRAT";
											String customString3 = "INR";
											String msgMarkC = "x0001";

											compensationMessage = '{' + "'__metadata': {" + "'uri': '" + msgMarkC
													+ "'}," + "'payGroup': '" + Carea + "', 'startDate': '" + startDate
													+ "', 'payGroup': '" + Carea + "', 'eventReason': '" + eventReason
													+ "', 'customString3': '" + customString3 + "'}";

											compensationMessage = compensationMessage.replaceAll("\\'", "\"");
											compensationMessage = compensationMessage.replace("x0001", metaPutComp);

											System.out.println("Post Compensation Thread started.... with msg "
													+ compensationMessage.toString());
											HttpPostRequest httpPostRequest = new HttpPostRequest(
													compensationMessage.toString());
											httpPostRequest.start();

											try {
												httpPostRequest.join();

											} catch (InterruptedException ie) {
											}

										}

										for (int k = 0; k < allowancesJSON.length(); k++) {

											JSONObject obj = allowancesJSON.getJSONObject(k);

											String myURI = obj.getString("href");

											String myCompValue = obj.getString("amount");
											Pattern p = Pattern.compile("\\'(.*?)\\'");
											Matcher m = p.matcher(myURI);
											m.find();
											String payComponent = m.group(1);

											if (payComponent.equals(Lgart + "_IND") && errorFlag == false) {

												String metaPut = "EmpPayCompRecurring(payComponent='" + payComponent
														+ "' ,userId='" + id + "')";
												metaPut = metaPut.replaceAll("\"", "\\'");
												String msgMark = "x0001";

												System.out.println(myCompValue);
												System.out.println(metaPut);
												System.out.println(payComponent);

												DecimalFormat df = new DecimalFormat("#.#");
												df.setRoundingMode(RoundingMode.CEILING);

												message = '{' + "'__metadata': {" + "'uri': '" + msgMark + "'},"
														+ "'paycompvalue': '" + myCompValue + "', 'startDate': '"
														+ startDate + "', 'customDouble1': '"
														+ df.format(Double.parseDouble(myCompValue) / fte) + "'}";

												message = message.replaceAll("\\'", "\"");
												message = message.replace("x0001", metaPut);
												System.out.println(message);
												System.out.println(Lgart);
												System.out.println("Pkind is:" + Pkind);

												if (Pkind.equals("2") || Pkind.equals("4")) {

													System.out.println("Carea is:" + Carea);

													switch (Carea) {

													case "I7":
														/*
														 * SAP INDIA
														 */

														if (!MaxFg.equals("0.00")) {

															if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																	|| Double.valueOf(myCompValue) < Double
																			.valueOf(MinFg)) {

																errorFlag = true;
															}

														}

														switch (Lgart) {

														case "4450":

															CarLeaseFound = true;

															break;

														case "42CF":

															TransportLeaseFound = true;

															break;

														case "42HF":

															if (id != null) {

																HttpRequest salaryThread = new HttpRequest(id,
																		"PayComp");
																salaryThread.start();

																try {

																	salaryThread.join();

																} catch (InterruptedException ie) {
																}

																String result = ((HttpRequest) salaryThread)
																		.getResult();

																JSONObject salaryAllowance = new JSONObject(result);
																JSONObject feed = salaryAllowance.getJSONObject("feed");

																JSONArray entries = feed.getJSONArray("entry");

																for (int j = 0; j < entries.length(); j++) {

																	JSONObject content = entries.getJSONObject(j);

																	JSONObject innerContent = content
																			.getJSONObject("content");

																	JSONObject properties = innerContent
																			.getJSONObject("m:properties");

																	String paycomponent = properties
																			.getString("d:payComponent");

																	if (paycomponent.equals("4B1F_IND")) {

																		System.out.println("We found 4B1F_IND");

																		Double salaryValue = Double.valueOf(properties
																				.getJSONObject("d:paycompvalue")
																				.getDouble("content"));

																		System.out.println(
																				"Salary Value is: " + salaryValue);

																		if (Metro.equals("Metro")) {

																			HouseRentLimit = salaryValue * 0.5;

																		} else {

																			HouseRentLimit = salaryValue * 0.4;
																		}

																		if (Double.valueOf(
																				myCompValue) > HouseRentLimit) {

																			errorFlag = true;

																			System.out.println(
																					"We got error from House Rent, limit is "
																							+ HouseRentLimit);
																		}

																	}

																}

															}

															break;

														}

														break;

													case "I8":
														/*
														 * ARIBA INDIA
														 */

														if (!MaxFg.equals("0.00")) {

															if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																	|| Double.valueOf(myCompValue) < Double
																			.valueOf(MinFg)) {

																errorFlag = true;
															}

														}

														switch (Lgart) {

														case "4450":

															CarLeaseFound = true;

															break;

														case "42CF":

															TransportLeaseFound = true;

															break;

														case "42HF":

															if (id != null) {

																HttpRequest salaryThread = new HttpRequest(id,
																		"PayComp");
																salaryThread.start();

																try {

																	salaryThread.join();

																} catch (InterruptedException ie) {
																}

																String result = ((HttpRequest) salaryThread)
																		.getResult();

																JSONObject salaryAllowance = new JSONObject(result);
																JSONObject feed = salaryAllowance.getJSONObject("feed");

																JSONArray entries = feed.getJSONArray("entry");

																for (int j = 0; j < entries.length(); j++) {

																	JSONObject content = entries.getJSONObject(j);

																	JSONObject innerContent = content
																			.getJSONObject("content");

																	JSONObject properties = innerContent
																			.getJSONObject("m:properties");

																	String paycomponent = properties
																			.getString("d:payComponent");

																	if (paycomponent.equals("4B1F_IND")) {

																		System.out.println("We found 4B1F_IND");

																		Double salaryValue = Double.valueOf(properties
																				.getJSONObject("d:paycompvalue")
																				.getDouble("content"));

																		System.out.println(
																				"Salary Value is: " + salaryValue);

																		if (Metro.equals("Metro")) {

																			HouseRentLimit = salaryValue * 0.5;

																		} else {

																			HouseRentLimit = salaryValue * 0.4;
																		}

																		if (Double.valueOf(
																				myCompValue) > HouseRentLimit) {

																			errorFlag = true;

																			System.out.println(
																					"We got error from House Rent, limit is "
																							+ HouseRentLimit);
																		}

																	}

																}

															}

															break;

														}

														break;

													case "I9":
														/*
														 * LABS INDIA
														 */
														System.out.println("We in Labs INDIA");

														if (!MaxFg.equals("0.00")) {

															if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																	|| Double.valueOf(myCompValue) < Double
																			.valueOf(MinFg)) {

																errorFlag = true;
																System.out
																		.println("Error Flag from MaxFG with" + MaxFg);
															}

														}

														switch (Lgart) {

														case "76CR":

															CarLeaseFound = true;

															break;

														case "76C1":

															TransportLeaseFound = true;

															break;

														case "76H1":

															if (id != null) {

																HttpRequest salaryThread = new HttpRequest(id,
																		"PayComp");
																salaryThread.start();

																try {

																	salaryThread.join();

																} catch (InterruptedException ie) {
																}

																String result = ((HttpRequest) salaryThread)
																		.getResult();

																JSONObject salaryAllowance = new JSONObject(result);
																JSONObject feed = salaryAllowance.getJSONObject("feed");

																JSONArray entries = feed.getJSONArray("entry");

																for (int j = 0; j < entries.length(); j++) {

																	JSONObject content = entries.getJSONObject(j);

																	JSONObject innerContent = content
																			.getJSONObject("content");

																	JSONObject properties = innerContent
																			.getJSONObject("m:properties");

																	String paycomponent = properties
																			.getString("d:payComponent");

																	if (paycomponent.equals("0003_GLO")) {

																		System.out.println("We found 0003_GLO");

																		Double salaryValue = Double.valueOf(properties
																				.getJSONObject("d:paycompvalue")
																				.getDouble("content"));

																		System.out.println(
																				"Salary Value is: " + salaryValue);

																		if (Metro.equals("Metro")) {

																			HouseRentLimit = salaryValue * 0.5;

																		} else {

																			HouseRentLimit = salaryValue * 0.4;
																		}

																		if (Double.valueOf(
																				myCompValue) > HouseRentLimit) {

																			errorFlag = true;

																			System.out.println(
																					"We got error from House Rent, limit is "
																							+ HouseRentLimit);
																		}

																	}

																}

															}

															break;

														}

														break;
													}

												} else if (Pkind != "2" && Pkind != "4") {

													errorFlag = true;
													System.out.println("Pkind is diferent than 2 or 4, it is:" + Pkind);
												}

												if (CarLeaseFound == true && TransportLeaseFound == true) {

													errorFlag = true;
												}

												System.out.println("Error Flag:" + errorFlag + "Pkind:" + Pkind);

												if (errorFlag == false) {

													System.out.println(
															"Post Thread started.... with msg " + message.toString());
													HttpPostRequest httpPostRequest = new HttpPostRequest(
															message.toString());
													httpPostRequest.start();

													try {
														httpPostRequest.join();

													} catch (InterruptedException ie) {
													}

													String result = httpPostRequest.getResult();

													response.getWriter().append(result);

												} else {

													response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

												}
											}

										}
									}

								}

							} catch (IOException e) {
								// In case of an IOException the connection will
								// be
								// released
								// back to the connection manager automatically
								throw e;
							} catch (RuntimeException e) {
								// In case of an unexpected exception you may
								// want
								// to abort
								// the HTTP request in order to shut down the
								// underlying
								// connection immediately.
								httpGet.abort();
								throw e;
							} finally {
								// Closing the input stream will trigger
								// connection
								// release
								try {
									instream.close();
								} catch (Exception e) {
									// Ignore
								}
							}
						}

					} catch (NamingException e) {
						// Lookup of destination failed
						String errorMessage = "Lookup of destination failed with reason: " + e.getMessage() + ". See "
								+ "logs for details. Hint: Make sure to have the destination " + destinationName
								+ " configured.";
						LOGGER.error("Lookup of destination failed", e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);

					} catch (Exception e) {
						// Connectivity operation failed
						String errorMessage = "Connectivity operation failed with reason: " + e.getMessage() + ". See "
								+ "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
								+ "local Eclipse environment in case your environment uses "
								+ "an HTTP proxy for the outbound Internet " + "communication.";
						LOGGER.error("Connectivity operation failed", e);
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
					} finally {
						// When HttpClient instance is no longer needed, shut
						// down
						// the connection manager to ensure immediate
						// deallocation of all system resources
						if (httpClient != null) {
							httpClient.getConnectionManager().shutdown();
						}
					}

				} else {

					LoginContext loginContext;
					try {
						loginContext = LoginContextFactory.createLoginContext("FORM");
						loginContext.login();

						HttpClient httpClient = null;
						String destinationName = "int_pg";
						System.out.println("Do Post Called");
						try {
							// Get HTTP destination
							Context ctx = new InitialContext();
							HttpDestination destination = null;
							if (destinationName != null) {
								DestinationFactory destinationFactory = (DestinationFactory) ctx
										.lookup(DestinationFactory.JNDI_NAME);
								destination = (HttpDestination) destinationFactory.getDestination("int_pg");
							} else {
								// The default request to the Servlet will use
								// outbound-internet-destination
								destinationName = "int_pg";
								destination = (HttpDestination) ctx.lookup("java:comp/env/" + destinationName);
							}

							// Create HTTP client
							httpClient = destination.createHttpClient();

							// Execute HTTP request
							HttpGet httpGet = new HttpGet("/sap/ZHR_SALARY_PACKAGING_HCP_SRV/ZT5W7ASet?$format=json");
							HttpResponse httpResponse = httpClient.execute(httpGet);

							HttpGet httpGetp0589 = new HttpGet(
									"/sap/ZHR_SALARY_PACKAGING_HCP_SRV/p0589Set?$format=json");
							HttpResponse httpResponsep0589 = httpClient.execute(httpGetp0589);

							// Check response status code
							int statusCode = httpResponse.getStatusLine().getStatusCode();
							if (statusCode != HTTP_OK) {
								throw new ServletException(
										"Expected response status code is 200 but it is " + statusCode + " .");
							}

							// Copy content from the incoming response to the
							// outgoing
							// response
							HttpEntity entity = httpResponse.getEntity();
							HttpEntity entityp0589 = httpResponsep0589.getEntity();

							if (entity != null && body != null) {
								InputStream instream = entity.getContent();
								try {
									byte[] buffer = new byte[COPY_CONTENT_BUFFER_SIZE];
									int len;

									body = body.replaceAll("\\r\\n", "");

									JSONArray allowancesJSON = new JSONArray(body);

									String responseString = EntityUtils.toString(entity, "UTF-8");

									JSONObject allowancesJSONIPP = new JSONObject(responseString);
									JSONObject d = allowancesJSONIPP.getJSONObject("d");
									JSONArray results = d.getJSONArray("results");

									String responseStringP0589 = EntityUtils.toString(entityp0589, "UTF-8");
									System.out.println("Do Post p0589");
									JSONObject p0589JSONIPP = new JSONObject(responseStringP0589);
									JSONObject p0589d = p0589JSONIPP.getJSONObject("d");
									JSONArray p0589results = p0589d.getJSONArray("results");
									System.out.println("Do Post p0589 after");

									String status = "Y";
									String existence = "Y";
									System.out.println("We here");
									for (int x = 0; x < p0589results.length(); x++) {

										JSONObject objp0589IPP = p0589results.getJSONObject(x);

										status = objp0589IPP.getString("Flag1");
										existence = objp0589IPP.getString("Flag2");

										if (status == "Y") {

											errorFlag = true;
										}

									}

									if (errorFlag == false) {

										for (int i = 0; i < results.length(); i++) {

											JSONObject objIPP = results.getJSONObject(i);

											id = objIPP.getString("UserId");
											String Carea = objIPP.getString("Carea");
											String Lgart = objIPP.getString("Lgart");
											String MaxFg = objIPP.getString("Maxfg");
											String MinFg = objIPP.getString("Minfg");
											String Pkind = objIPP.getString("Pkind");
											String Metro = objIPP.getString("Area");

											if (id != null) {

												HttpRequest jobThread = new HttpRequest(id, "EmpJob");
												jobThread.start();

												try {

													jobThread.join();

												} catch (InterruptedException ie) {
												}

												String result = ((HttpRequest) jobThread).getResult();

												JSONObject jobAllowance = new JSONObject(result);
												JSONObject feed = jobAllowance.getJSONObject("feed");

												JSONObject entries = feed.getJSONObject("entry");

												JSONObject content = entries.getJSONObject("content");

												JSONObject properties = content.getJSONObject("m:properties");

												JSONObject fteContent = properties.getJSONObject("d:fte");

												fte = fteContent.getDouble("content");

												System.out.println("We found fte field:" + fte);

											}

											int year = Calendar.getInstance().get(Calendar.YEAR);
											int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
											int dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
											String month;
											String day;

											if (currentMonth < 10) {

												month = "0" + String
														.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);

											} else {

												month = String.valueOf(currentMonth);
											}

											if (dayOfMonth < 10) {

												day = "0" + String
														.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

											} else {

												day = String.valueOf(dayOfMonth);

											}

											if (existence == "Y") {

												/*
												 * User exists in previous SPM
												 */

												if (currentMonth == 4) {

													if (dayOfMonth < 21) {

														startDate = year + "/" + month + "/01";

													} else {

														currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

														if (currentMonth < 10) {

															month = "0" + String.valueOf(
																	Calendar.getInstance().get(Calendar.MONTH) + 1);

														} else {

															month = String.valueOf(currentMonth);
														}

														if (currentMonth == 13) {

															month = "01";
															year++;
														}

														startDate = year + "/" + month + "/01";
													}

												} else {

													if (dayOfMonth < 16) {

														startDate = year + "/" + month + "/" + day;

													} else {

														currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

														if (currentMonth < 10) {

															month = "0" + String.valueOf(
																	Calendar.getInstance().get(Calendar.MONTH) + 1);

														} else {

															month = String.valueOf(currentMonth);
														}

														if (currentMonth == 13) {

															month = "01";
															year++;
														}

														startDate = year + "/" + month + "/01";
													}

												}

											} else {

												/*
												 * User does not exist in
												 * previous SPM
												 */

												if (dayOfMonth < 16) {

													startDate = year + "/" + month + "/01";

												} else {

													currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 2;

													if (currentMonth < 10) {

														month = "0" + String.valueOf(
																Calendar.getInstance().get(Calendar.MONTH) + 1);

													} else {

														month = String.valueOf(currentMonth);
													}

													if (currentMonth == 13) {

														month = "01";
														year++;
													}

													startDate = year + "/" + month + "/01";
												}

											}

											System.out.println("After dates2");
											System.out.println(allowancesJSON.toString());
											String startDateCompare = startDate.replaceAll("/", "-") + "T00:00:00";

											DateFormat formatter;
											formatter = new SimpleDateFormat("yyyy/MM/dd");

											Date date = formatter.parse(startDate);

											System.out.println(date.getTime() * 1000);
											Long l = new Long(date.getTime());
											startDate = "/Date(" + l.toString() + ")/";
											System.out.println("Startdate is : " + startDate);

											HttpRequest compensationThread = new HttpRequest(id, "EmpCompensation");
											compensationThread.start();

											try {

												compensationThread.join();

											} catch (InterruptedException ie) {
											}

											String resultComp = ((HttpRequest) compensationThread).getResult();

											System.out.println("Compensation result is:" + resultComp.toString());
											JSONObject compensationAllowance = new JSONObject(resultComp);
											JSONObject compensationFeed = compensationAllowance.getJSONObject("feed");

											JSONObject compensationEntry = compensationFeed.getJSONObject("entry");

											JSONObject compensationInnerContent = compensationEntry
													.getJSONObject("content");

											JSONObject compensationProperties = compensationInnerContent
													.getJSONObject("m:properties");

											String compensationStartDate = compensationProperties
													.getJSONObject("d:startDate").getString("content");

											System.out.println("Compensation date is: " + compensationStartDate
													+ " and cutoffDate is: " + startDateCompare);

											if (!compensationStartDate.equals(startDateCompare)) {

												System.out.println("Updating Employee compensation row...");

												String metaPutComp = "EmpCompensation(userId='" + id
														+ "' , startDate=datetime'" + compensationStartDate + "')";
												metaPutComp = metaPutComp.replaceAll("\"", "\\'");
												String eventReason = "Z_D_MIGRAT";
												String customString3 = "INR";
												String msgMarkC = "x0001";

												compensationMessage = '{' + "'__metadata': {" + "'uri': '" + msgMarkC
														+ "'}," + "'payGroup': '" + Carea + "', 'startDate': '"
														+ startDate + "', 'payGroup': '" + Carea + "', 'eventReason': '"
														+ eventReason + "', 'customString3': '" + customString3 + "'}";

												message = message.replaceAll("\\'", "\"");
												message = message.replace("x0001", metaPutComp);

												System.out.println("Post Compensation Thread started.... with msg "
														+ compensationMessage.toString());
												HttpPostRequest httpPostRequest = new HttpPostRequest(
														compensationMessage.toString());
												httpPostRequest.start();

												try {
													httpPostRequest.join();

												} catch (InterruptedException ie) {
												}

											}

											for (int k = 0; k < allowancesJSON.length(); k++) {

												JSONObject obj = allowancesJSON.getJSONObject(k);

												String myURI = obj.getString("href");

												String myCompValue = obj.getString("amount");
												Pattern p = Pattern.compile("\\'(.*?)\\'");
												Matcher m = p.matcher(myURI);
												m.find();
												String payComponent = m.group(1);

												if (payComponent.equals(Lgart + "_IND") && errorFlag == false) {

													String metaPut = "EmpPayCompRecurring(payComponent='" + payComponent
															+ "' ,userId='" + id + "')";
													metaPut = metaPut.replaceAll("\"", "\\'");
													String msgMark = "x0001";

													System.out.println(myCompValue);
													System.out.println(metaPut);
													System.out.println(payComponent);

													DecimalFormat df = new DecimalFormat("#.#");
													df.setRoundingMode(RoundingMode.CEILING);

													message = '{' + "'__metadata': {" + "'uri': '" + msgMark + "'},"
															+ "'paycompvalue': '" + myCompValue + "', 'startDate': '"
															+ startDate + "', 'customDouble1': '"
															+ df.format(Double.parseDouble(myCompValue) / fte) + "'}";

													message = message.replaceAll("\\'", "\"");
													message = message.replace("x0001", metaPut);
													System.out.println(message);
													System.out.println(Lgart);
													System.out.println("Pkind is:" + Pkind);

													if (Pkind.equals("2") || Pkind.equals("4")) {

														System.out.println("Carea is:" + Carea);

														switch (Carea) {

														case "I7":
															/*
															 * SAP INDIA
															 */

															if (!MaxFg.equals("0.00")) {

																if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																		|| Double.valueOf(myCompValue) < Double
																				.valueOf(MinFg)) {

																	errorFlag = true;
																}

															}

															switch (Lgart) {

															case "4450":

																CarLeaseFound = true;

																break;

															case "42CF":

																TransportLeaseFound = true;

																break;

															case "42HF":

																if (id != null) {

																	HttpRequest salaryThread = new HttpRequest(id,
																			"PayComp");
																	salaryThread.start();

																	try {

																		salaryThread.join();

																	} catch (InterruptedException ie) {
																	}

																	String result = ((HttpRequest) salaryThread)
																			.getResult();

																	JSONObject salaryAllowance = new JSONObject(result);
																	JSONObject feed = salaryAllowance
																			.getJSONObject("feed");

																	JSONArray entries = feed.getJSONArray("entry");

																	for (int j = 0; j < entries.length(); j++) {

																		JSONObject content = entries.getJSONObject(j);

																		JSONObject innerContent = content
																				.getJSONObject("content");

																		JSONObject properties = innerContent
																				.getJSONObject("m:properties");

																		String paycomponent = properties
																				.getString("d:payComponent");

																		if (paycomponent.equals("4B1F_IND")) {

																			System.out.println("We found 4B1F_IND");

																			Double salaryValue = Double
																					.valueOf(properties
																							.getJSONObject(
																									"d:paycompvalue")
																							.getDouble("content"));

																			System.out.println(
																					"Salary Value is: " + salaryValue);

																			if (Metro.equals("Metro")) {

																				HouseRentLimit = salaryValue * 0.5;

																			} else {

																				HouseRentLimit = salaryValue * 0.4;
																			}

																			if (Double.valueOf(
																					myCompValue) > HouseRentLimit) {

																				errorFlag = true;

																				System.out.println(
																						"We got error from House Rent, limit is "
																								+ HouseRentLimit);
																			}

																		}

																	}

																}

																break;

															}

															break;

														case "I8":
															/*
															 * ARIBA INDIA
															 */

															if (!MaxFg.equals("0.00")) {

																if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																		|| Double.valueOf(myCompValue) < Double
																				.valueOf(MinFg)) {

																	errorFlag = true;
																}

															}

															switch (Lgart) {

															case "4450":

																CarLeaseFound = true;

																break;

															case "42CF":

																TransportLeaseFound = true;

																break;

															case "42HF":

																if (id != null) {

																	HttpRequest salaryThread = new HttpRequest(id,
																			"PayComp");
																	salaryThread.start();

																	try {

																		salaryThread.join();

																	} catch (InterruptedException ie) {
																	}

																	String result = ((HttpRequest) salaryThread)
																			.getResult();

																	JSONObject salaryAllowance = new JSONObject(result);
																	JSONObject feed = salaryAllowance
																			.getJSONObject("feed");

																	JSONArray entries = feed.getJSONArray("entry");

																	for (int j = 0; j < entries.length(); j++) {

																		JSONObject content = entries.getJSONObject(j);

																		JSONObject innerContent = content
																				.getJSONObject("content");

																		JSONObject properties = innerContent
																				.getJSONObject("m:properties");

																		String paycomponent = properties
																				.getString("d:payComponent");

																		if (paycomponent.equals("4B1F_IND")) {

																			System.out.println("We found 4B1F_IND");

																			Double salaryValue = Double
																					.valueOf(properties
																							.getJSONObject(
																									"d:paycompvalue")
																							.getDouble("content"));

																			System.out.println(
																					"Salary Value is: " + salaryValue);

																			if (Metro.equals("Metro")) {

																				HouseRentLimit = salaryValue * 0.5;

																			} else {

																				HouseRentLimit = salaryValue * 0.4;
																			}

																			if (Double.valueOf(
																					myCompValue) > HouseRentLimit) {

																				errorFlag = true;

																				System.out.println(
																						"We got error from House Rent, limit is "
																								+ HouseRentLimit);
																			}

																		}

																	}

																}

																break;

															}

															break;

														case "I9":
															/*
															 * LABS INDIA
															 */

															System.out.println("We in Labs INDIA");

															if (!MaxFg.equals("0.00")) {

																if (Double.valueOf(myCompValue) > Double.valueOf(MaxFg)
																		|| Double.valueOf(myCompValue) < Double
																				.valueOf(MinFg)) {

																	errorFlag = true;

																	System.out.println(
																			"We got error from MaxFG with" + MaxFg);
																}

															}

															switch (Lgart) {

															case "76CR":

																CarLeaseFound = true;

																break;

															case "76C1":

																TransportLeaseFound = true;

																break;

															case "76H1":
																System.out.println("We in 76H1_IND");

																HttpRequest salaryThread = new HttpRequest(id,
																		"PayComp");
																salaryThread.start();

																try {

																	salaryThread.join();

																} catch (InterruptedException ie) {
																}

																String result = ((HttpRequest) salaryThread)
																		.getResult();

																JSONObject salaryAllowance = new JSONObject(result);
																JSONObject feed = salaryAllowance.getJSONObject("feed");

																JSONArray entries = feed.getJSONArray("entry");

																for (int j = 0; j < entries.length(); j++) {

																	JSONObject content = entries.getJSONObject(j);

																	JSONObject innerContent = content
																			.getJSONObject("content");

																	JSONObject properties = innerContent
																			.getJSONObject("m:properties");

																	String paycomponent = properties
																			.getString("d:payComponent");

																	if (paycomponent.equals("0003_GLO")) {

																		System.out.println("We found 0003_GLO");

																		Double salaryValue = Double.valueOf(properties
																				.getJSONObject("d:paycompvalue")
																				.getDouble("content"));

																		System.out.println(
																				"Salary Value is: " + salaryValue);

																		if (Metro.equals("Metro")) {

																			HouseRentLimit = salaryValue * 0.5;

																		} else {

																			HouseRentLimit = salaryValue * 0.4;
																		}

																		if (Double.valueOf(
																				myCompValue) > HouseRentLimit) {

																			errorFlag = true;

																			System.out.println(
																					"We got error from House Rent, limit is "
																							+ HouseRentLimit);

																		}

																	}

																}

																break;

															}

															break;
														}

													} else if (Pkind != "2" && Pkind != "4") {

														errorFlag = true;
														System.out.println(
																"Pkind is diferent than 2 or 4, it is:" + Pkind);
													}

													if (CarLeaseFound == true && TransportLeaseFound == true) {

														errorFlag = true;
													}

													System.out.println("Error Flag:" + errorFlag + "Pkind:" + Pkind);

													if (errorFlag == false) {

														System.out.println("Post Thread started.... with msg "
																+ message.toString());

														HttpPostRequest httpPostRequest = new HttpPostRequest(
																message.toString());
														httpPostRequest.start();

														try {
															httpPostRequest.join();

														} catch (InterruptedException ie) {
														}

														String result = httpPostRequest.getResult();

													} else {

														response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

													}
												}

											}

										}

									}

								} catch (IOException e) {
									// In case of an IOException the connection
									// will be
									// released
									// back to the connection manager
									// automatically
									throw e;
								} catch (RuntimeException e) {
									// In case of an unexpected exception you
									// may want
									// to abort
									// the HTTP request in order to shut down
									// the
									// underlying
									// connection immediately.
									httpGet.abort();
									throw e;
								} finally {
									// Closing the input stream will trigger
									// connection
									// release
									try {
										instream.close();
									} catch (Exception e) {
										// Ignore
									}
								}
							}

						} catch (NamingException e) {
							// Lookup of destination failed
							String errorMessage = "Lookup of destination failed with reason: " + e.getMessage()
									+ ". See " + "logs for details. Hint: Make sure to have the destination "
									+ destinationName + " configured.";
							LOGGER.error("Lookup of destination failed", e);
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);

						} catch (Exception e) {
							// Connectivity operation failed
							String errorMessage = "Connectivity operation failed with reason: " + e.getMessage()
									+ ". See "
									+ "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
									+ "local Eclipse environment in case your environment uses "
									+ "an HTTP proxy for the outbound Internet " + "communication.";
							LOGGER.error("Connectivity operation failed", e);
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
						} finally {
							// When HttpClient instance is no longer needed,
							// shut down
							// the connection manager to ensure immediate
							// deallocation of all system resources
							if (httpClient != null) {
								httpClient.getConnectionManager().shutdown();
							}
						}

					} catch (Exception e) {

					}

				}

			} catch (ParseException e) {
				// crash and burn
				throw new IOException("Error parsing JSON request string");
			}

			System.out.println("Done Post");
			
			//Email part - uncomment when STPM is configured
			
			/*
			 * 
			 if (errorFlag == false) {

			HttpRequest mailThread = new HttpRequest(id, "PerEmail", startDateReq, startDateFlag);
			mailThread.start();

			try {

				mailThread.join();

			} catch (InterruptedException ie) {
			}

			String result = ((HttpRequest) mailThread).getResult();

			JSONObject emailAllowance = new JSONObject(result);
			JSONObject feed = emailAllowance.getJSONObject("feed");
			JSONObject entry = feed.getJSONObject("entry");

			JSONObject content = entry.getJSONObject("content");
			JSONObject properties = content.getJSONObject("m:properties");
			String email = properties.getString("d:emailAddress");

	
			 * System.out.println("Sending confirmation mail...");
			 * 
			 * // SendMailSSL mail = new SendMailSSL(); try {
			 * SendMailSSL.Send("username", "password",
			 * email, "",
			 * "Salary Package Modeller - Submition Confirmation", mailBody); }
			 * catch (MessagingException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 * 
			 * } else {
			 * 
			 * System.out.println("Sending error mail...");
			 * 
			 * // SendMailSSL mail = new SendMailSSL(); try {
			 * SendMailSSL.Send("username", "password",
			 * email, "",
			 * "Salary Package Modeller - Error Notification",
			 * "There was an error in your submition, please contact an administrator. Thank you."
			 * ); } catch (MessagingException e) { // TODO Auto-generated catch
			 * block e.printStackTrace(); } }
			 */

		}

	}

	public static String getBody(HttpServletRequest request) throws IOException {

		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}

		body = stringBuilder.toString();
		return body;
	}

}
