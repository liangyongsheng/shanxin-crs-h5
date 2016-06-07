package com.shanxin.crs.h5.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanxin.core.util.MyStringUtils;
import com.shanxin.crs.h5.entity.DoResponse;
import com.shanxin.crs.h5.entity.Oprt;
import com.shanxin.oprt.serivce.entity.request.OprtLoginRequest;
import com.shanxin.oprt.serivce.entity.response.OprtLoginResponse;

@Controller
@RequestMapping
public class IndexController extends BaseController {

	@RequestMapping("/index")
	public ModelAndView index() {
		ModelAndView mv = new ModelAndView();
		Oprt oprt = (Oprt) this.getSessionValue(BaseController.SESSION_OPRT);
		if (oprt == null)
			mv.setViewName("redirect:/login.jsp");
		else
			mv.setViewName("redirect:/meb/index.jsp");
		return mv;
	}

	@RequestMapping("/login")
	public ModelAndView login() {
		ModelAndView mv = new ModelAndView();
		Oprt oprt = (Oprt) this.getSessionValue(BaseController.SESSION_OPRT);
		if (oprt != null)
			mv.setViewName("redirect:/meb/index.jsp");
		else
			mv.setViewName("login");
		return mv;
	}

	@RequestMapping("/logout")
	public ModelAndView logout() {
		ModelAndView mv = new ModelAndView();
		this.cleanSession();
		mv.setViewName("redirect:/login.jsp");
		return mv;
	}

	@RequestMapping(path = "/do_login", method = RequestMethod.POST)
	public void do_login(HttpServletResponse response) throws Exception {
		boolean check = false;
		String msg = null;

		try {
			String name = this.getParameterValue("name", String.class, null);
			String pwd = this.getParameterValue("pwd", String.class, null);

			if (MyStringUtils.isEmpty(name))
				throw new Exception("用户名为空");
			if (MyStringUtils.isEmpty(pwd))
				throw new Exception("用户密码为空");

			OprtLoginRequest request = new OprtLoginRequest();
			request.setOprtLoginName(name);
			request.setOprtLoginPwd(pwd);
			OprtLoginResponse rsp = this.request("oprtSvc", request);

			Oprt oprt = new Oprt();
			oprt.setOprtId(rsp.getOprtId());
			oprt.setOprtSecret(rsp.getOprtSecret());
			oprt.setOprtAccessToken(rsp.getOprtAccessToken());
			this.setSessionValue(BaseController.SESSION_OPRT, oprt);
			check = true;
		} catch (Throwable ex) {
			msg = ex.getMessage();
		}

		DoResponse doRsp = new DoResponse();
		doRsp.setSuccess(check);
		doRsp.setMsg(msg);
		ObjectMapper om = new ObjectMapper();
		String rspStr = om.writeValueAsString(doRsp);

		// 写回
		response.setHeader("Content-type", "application/json; charset=utf-8");
		response.getWriter().write(rspStr);
		response.getWriter().flush();
	}
}
