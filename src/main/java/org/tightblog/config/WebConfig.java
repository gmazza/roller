/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tightblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor;
import org.springframework.mobile.device.DeviceWebArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesView;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.tightblog.rendering.model.Model;
import org.tightblog.rendering.model.SiteModel;
import org.tightblog.rendering.model.URLModel;
import org.tightblog.rendering.model.UtilitiesModel;
import org.tightblog.rendering.requests.WeblogPageRequest;
import org.tightblog.rendering.thymeleaf.ThemeTemplateResolver;
import org.tightblog.rendering.thymeleaf.ThymeleafRenderer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/tb-ui/app/get-default-blog");
    }

    @Bean
    public UrlBasedViewResolver tilesViewResolver() {
        UrlBasedViewResolver tilesViewResolver = new UrlBasedViewResolver();
        tilesViewResolver.setViewClass(TilesView.class);
        tilesViewResolver.setCacheUnresolved(false);
        tilesViewResolver.setOrder(0);
        return tilesViewResolver;
    }

    @Bean
    public TilesConfigurer tilesConfigurer() {
        TilesConfigurer tconf = new TilesConfigurer();
        tconf.setDefinitions("/WEB-INF/tiles.xml");
        return tconf;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Adding Spring mobile argument resolvers (allowing use of Device argument in Controller methods)
        resolvers.add(
                new ServletWebArgumentResolverAdapter(
                        new DeviceWebArgumentResolver()));
    }

    // To process resources in the webapp/thymeleaf folder: Atom feeds,
    // common blog theme elements, emails, etc.
    @Bean
    public SpringResourceTemplateResolver standardTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/thymeleaf/");
        resolver.setSuffix(".html");
        // 1 reserved for the ThemeTemplateResolver (blog theme-specific templates)
        resolver.setOrder(2);
        // not modifiable, so can cache
        resolver.setCacheable(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine blogTemplateEngine(ThemeTemplateResolver themeTemplateResolver,
                                                   SpringResourceTemplateResolver standardTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        Set<ITemplateResolver> templateResolvers = new HashSet<>();
        templateResolvers.add(themeTemplateResolver);
        templateResolvers.add(standardTemplateResolver);
        engine.setTemplateResolvers(templateResolvers);
        return engine;
    }

    @Bean
    public SpringTemplateEngine standardTemplateEngine(SpringResourceTemplateResolver standardTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(standardTemplateResolver);
        return engine;
    }

    /**
     * Standard renderer uses a hardcoded template independent of any specific blog.
     * Examples: Atom feeds and the ExternalSourceController (embedded GitHub code).
     */
    @Bean
    public ThymeleafRenderer standardRenderer(SpringTemplateEngine standardTemplateEngine) {
        ThymeleafRenderer tr = new ThymeleafRenderer();
        tr.setTemplateEngine(standardTemplateEngine);
        return tr;
    }

    /**
     * The blog renderer is for generation of blog-specific pages, and has a more complex
     * strategy for determining the proper templates to use (default or blogger-overridden),
     */
    @Bean
    public ThymeleafRenderer blogRenderer(SpringTemplateEngine blogTemplateEngine) {
        ThymeleafRenderer tr = new ThymeleafRenderer();
        tr.setTemplateEngine(blogTemplateEngine);
        return tr;
    }

    @Bean
    public Function<WeblogPageRequest, SiteModel> siteModelFactory() {
        return this::siteModel;
    }

    @Bean
    @Scope("prototype")
    public SiteModel siteModel(WeblogPageRequest wpr) {
        return new SiteModel(wpr);
    }

    @Bean
    @Scope("prototype")
    public Set<Model> pageModelSet(UtilitiesModel utilitiesModel, URLModel urlModel) {
        Set<Model> models = new HashSet<>();
        models.add(utilitiesModel);
        models.add(urlModel);
        return models;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DeviceResolverHandlerInterceptor());
    }
}
