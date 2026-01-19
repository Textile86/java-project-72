@import hexlet.code.dto.UrlsPage
@import hexlet.code.model.NamedRoutes
@import java.text.SimpleDateFormat
@param UrlsPage page

@template.layout.page(
content = @`
    @if(page.getFlash() != null)
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            ${page.getFlash()}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    @endif

    @if(page.getFlashError() != null)
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            ${page.getFlashError()}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    @endif

    <h1>${page.getHeader()}</h1>

    <div class="table-responsive mt-4">
        <table class="table table-bordered table-hover">
            <thead>
            <tr>
                <th>ID</th>
                <th>Имя</th>
                <th>Последняя проверка</th>
                <th>Код ответа</th>
            </tr>
            </thead>
            <tbody>
            @if(page.getUrls().isEmpty())
                <tr>
                    <td colspan="4" class="text-center">
                        @if(page.getTerm() == null || page.getTerm().isEmpty())
                            Пока не добавлено ни одного сайта
                        @else
                            По запросу "${page.getTerm()}" ничего не найдено
                        @endif
                    </td>
                </tr>
            @else
                @for(var url : page.getUrls())
                    <tr>
                        <td>${url.getId()}</td>
                        <td>
                            <a href="${NamedRoutes.urlPath(url.getId().toString())}">
                                ${url.getName()}
                            </a>
                        </td>
                        <td>
                            @if(page.getLatestChecks() != null && page.getLatestChecks().get(url.getId()) != null)
                                ${new SimpleDateFormat("dd/MM/yyyy HH:mm").format(page.getLatestChecks().get(url.getId()).getCreatedAt())}
                            @endif
                        </td>
                        <td>
                            @if(page.getLatestChecks() != null && page.getLatestChecks().get(url.getId()) != null)
                                ${page.getLatestChecks().get(url.getId()).getStatusCode()}
                            @endif
                        </td>
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
`
)