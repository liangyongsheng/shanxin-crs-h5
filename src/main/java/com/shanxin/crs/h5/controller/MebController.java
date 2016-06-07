package com.shanxin.crs.h5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.shanxin.crs.h5.entity.Oprt;
import com.shanxin.meb.service.entity.request.MebQueryRequest;
import com.shanxin.meb.service.entity.response.MebQueryResponse;

@Controller
@RequestMapping("/meb")
public class MebController extends BaseController {

	@RequestMapping("/index")
	public ModelAndView index() throws Exception {
		ModelAndView mv = new ModelAndView();
		Oprt oprt = (Oprt) this.getSessionValue(BaseController.SESSION_OPRT);
		if (oprt == null) {
			mv.setViewName("redirect:/login.jsp");
			return mv;
		}

		mv.setViewName("meb/index");
		MebQueryRequest request = new MebQueryRequest();
		MebQueryResponse response = this.request("mebSvc", request);
		mv.addObject("mebs", response.getMebs());
		return mv;
	}
}
