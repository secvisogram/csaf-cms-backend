package de.bsi.secvisogram.csaf_cms_backend;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Do not perform any post construct actions during test
 */
@Primary
@Component
public class PostConstructActions {}
