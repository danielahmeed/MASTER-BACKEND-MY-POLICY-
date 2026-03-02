package com.mypolicy.pipeline.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects root and /insurer-portal to the Insurer Portal UI.
 */
@Controller
public class PortalController {

  @GetMapping({"/", "/insurer-portal", "/insurer-portal/"})
  public String index() {
    return "redirect:/insurer-portal/index.html";
  }
}
