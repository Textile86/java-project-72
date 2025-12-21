package gg.jte.generated.ondemand;
public final class JteindexGenerated {
	public static final String JTE_NAME = "index.jte";
	public static final int[] JTE_LINE_INFO = {0,0,0,0,0,2,2,17,17,17,17,17,17,17,17};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor) {
		gg.jte.generated.ondemand.layout.JtepageGenerated.render(jteOutput, jteHtmlInterceptor, new gg.jte.html.HtmlContent() {
			public void writeTo(gg.jte.html.HtmlTemplateOutput jteOutput) {
				jteOutput.writeContent("\r\n    <div class=\"row\">\r\n        <div class=\"col-md-8 mx-auto\">\r\n            <h1 class=\"mb-4\">Анализатор страниц</h1>\r\n            <p class=\"lead\">Бесплатно проверяйте сайты на SEO пригодность</p>\r\n\r\n            <form action=\"/urls\" method=\"post\" class=\"mt-4\">\r\n                <div class=\"input-group mb-3\">\r\n                    <input type=\"text\" name=\"url\" class=\"form-control\"\r\n                           placeholder=\"https://www.example.com\" required>\r\n                    <button type=\"submit\" class=\"btn btn-primary\">Проверить</button>\r\n                </div>\r\n            </form>\r\n        </div>\r\n    </div>\r\n");
			}
		});
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		render(jteOutput, jteHtmlInterceptor);
	}
}
