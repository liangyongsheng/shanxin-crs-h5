package com.shanxin.crs.h5.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.shanxin.core.api.ApiRequest;
import com.shanxin.core.api.ApiResponse;
import com.shanxin.core.api.Response;
import com.shanxin.core.context.MyContext;
import com.shanxin.core.util.MyAlgorithmUtils;
import com.shanxin.core.util.MyStringUtils;
import com.shanxin.crs.h5.entity.Oprt;

@Controller
public abstract class BaseController {
	protected final static String SESSION_OPRT = "SESSION_OPRT";

	protected void setSessionValue(String key, Object value) {
		HttpServletRequest request = MyContext.getHttpServletRequest();
		if (request == null)
			return;

		@SuppressWarnings("unchecked")
		Map<String, Object> mp = (Map<String, Object>) request.getSession().getAttribute("my_All!@#123");
		if (mp == null)
			mp = new HashMap<String, Object>();
		mp.put(key, value);
		request.getSession().setAttribute("my_All!@#123", mp);
	}

	protected Object getSessionValue(String key) {
		HttpServletRequest request = MyContext.getHttpServletRequest();
		if (request == null)
			return null;

		@SuppressWarnings("unchecked")
		Map<String, Object> mp = (Map<String, Object>) request.getSession().getAttribute("my_All!@#123");
		if (mp == null)
			mp = new HashMap<String, Object>();
		if (mp.containsKey(key))
			return mp.get(key);
		else
			return null;
	}

	protected void cleanSession() {
		HttpServletRequest request = MyContext.getHttpServletRequest();
		if (request == null)
			return;
		try {
			Enumeration<String> er = request.getSession().getAttributeNames();
			while (er.hasMoreElements()) {
				String str = er.nextElement();
				if (str.startsWith("my_"))
					request.getSession().removeAttribute(str);
			}
		} catch (Throwable ex) {
		}
	}

	/**
	 * 取得请求的参数值
	 * 
	 * @param parameterName
	 * @param theClass
	 * @param defaultObj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getParameterValue(String parameterName, Class<T> theClass, T defaultObj) {
		T rs = null;
		SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sfdDate = new SimpleDateFormat("yyyy-MM-dd");

		try {
			HttpServletRequest request = MyContext.getHttpServletRequest();
			if (request == null)
				return rs == null ? defaultObj : rs;

			String objStr = MyStringUtils.isEmpty(request.getParameter(parameterName)) ? null : request.getParameter(parameterName);
			if (objStr != null) {
				if (String.class.isAssignableFrom(theClass))
					objStr = "\"" + objStr + "\"";
				if (Date.class.isAssignableFrom(theClass)) {
					if (!objStr.trim().equals("")) {
						if (objStr.length() > 10)
							rs = (T) sfd.parse(objStr);
						else
							rs = (T) sfdDate.parse(objStr);
					}
				} else {
					ObjectMapper om = new ObjectMapper();
					om.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
					rs = om.readValue(objStr, theClass);
				}
			}
		} catch (Throwable ex) {
		}
		return rs == null ? defaultObj : rs;
	}

	/**
	 * 向接口请求数据
	 * 
	 * @param svcName
	 * @param request
	 * @return
	 * @throws Exception
	 */
	protected <T extends ApiResponse> T request(String svcName, ApiRequest<T> request) throws Exception {
		try {

			@SuppressWarnings("unchecked")
			Map<String, String> hm = (Map<String, String>) MyContext.getRootApplicationContext().getBean(svcName);

			// 设置参数
			request.setMethod(request.getLocalMothedName());
			request.setTimestamp(new Date());
			Oprt oprt = (Oprt) this.getSessionValue(BaseController.SESSION_OPRT);
			if (oprt != null) {
				request.setOprtId(oprt.getOprtId());
				request.setOprtSecret(oprt.getOprtSecret());
				request.setOprtAccessToken(oprt.getOprtAccessToken());
			}

			String postData = "";
			ObjectMapper om = new ObjectMapper();
			om.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
			postData = om.writeValueAsString(request);
			// SIGN
			String sign = MyAlgorithmUtils.MD5(postData);
			// URL
			URL url = new URL(hm.get("url"));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-type", MediaType.APPLICATION_JSON_VALUE);
			connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
			connection.setRequestProperty("Sign", sign);
			connection.setConnectTimeout(5 * 60 * 1000);
			connection.setReadTimeout(5 * 60 * 1000);
			connection.setUseCaches(false);
			connection.setDoInput(true);

			// send request
			if (postData != null && !postData.equals("")) {
				connection.setDoOutput(true);
				OutputStreamWriter os = new OutputStreamWriter(connection.getOutputStream(), Charset.forName("utf-8"));
				os.write(postData);
				os.flush();
				os.close();
			}

			// get response
			Response<T> response = null;
			InputStream inputStream = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("utf-8")));
			String line = null;
			StringBuffer rspSb = new StringBuffer();
			while ((line = rd.readLine()) != null)
				rspSb.append(line);

			// close and disconnect
			inputStream.close();
			connection.disconnect();

			// json ---> object
			TypeFactory tf = TypeFactory.defaultInstance();
			JavaType jt = tf.constructParametricType(Response.class, request.getApiResponseType());
			om.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
			response = om.readValue(rspSb.toString(), jt);
			if (!response.isSuccess())
				throw new Exception(response.getMsg());
			return response.getResult();
		} catch (Throwable ex) {
			throw new Exception(ex.getMessage());
		}
	}

}
