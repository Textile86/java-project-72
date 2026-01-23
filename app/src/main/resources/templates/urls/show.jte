@import hexlet.code.dto.UrlPage
@import hexlet.code.model.NamedRoutes
@import java.time.format.DateTimeFormatter
@param UrlPage page

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

    <h1>Сайт: ${page.getUrl().getName()}</h1>

    <div class="table-responsive mt-4">
        <table class="table table-bordered">
            <tbody>
            <tr>
                <td>ID</td>
                <td>${page.getUrl().getId()}</td>
            </tr>
            <tr>
                <td>Имя</td>
                <td>${page.getUrl().getName()}</td>
            </tr>
            <tr>
                <td>Дата создания</td>
                <td>${page.getUrl().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}</td>
            </tr>
            </tbody>
        </table>
    </div>

    <h2 class="mt-5">Проверки</h2>
    <form action="${NamedRoutes.urlPath(page.getUrl().getId().toString())}/checks" method="post">
        <button type="submit" class="btn btn-primary">Запустить проверку</button>
    </form>

    <div class="table-responsive mt-4">
        <table class="table table-bordered table-hover">
            <thead>
            <tr>
                <th>ID</th>
                <th>Код ответа</th>
                <th>title</th>
                <th>h1</th>
                <th>description</th>
                <th>Дата проверки</th>
            </tr>
            </thead>
            <tbody>
            @if(page.getChecks().isEmpty())
                <tr>
                    <td colspan="6" class="text-center">Проверок пока не было</td>
                </tr>
            @else
                @for(var check : page.getChecks())
                    <tr>
                        <td>${check.getId()}</td>
                        <td>${check.getStatusCode()}</td>
                        <td>${check.getTitle() != null ? check.getTitle() : ""}</td>
                        <td>${check.getH1() != null ? check.getH1() : ""}</td>
                        <td>${check.getDescription() != null ? check.getDescription() : ""}</td>
                        <td>${check.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}</td>
                    </tr>
                @endfor
            @endif
            </tbody>
        </table>
    </div>
`
)