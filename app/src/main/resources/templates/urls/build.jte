@import hexlet.code.dto.BuildUrlPage
@import hexlet.code.model.NamedRoutes
@param BuildUrlPage page

@template.layout.page(
content = @`
    @if(page.getErrors() != null)
        <ul>
            @for(var validator : page.getErrors().values())
                @for(var error : validator)
                    <li>${error.getMessage()}</li>
                @endfor
            @endfor
        </ul>
    @endif

    <form action="${NamedRoutes.urlsPath()}" method="post">
        <div>
            <label>
                Название сайта
                <input type="text" name="url" value="${page.getName()}" />
            </label>
        </div>
        <div>
            <label>
                Описание курса
                <input type="text" required name="description" value="${page.getDescription()}" />
            </label>
        </div>
        <input type="submit" value="Добавить курс" />
    </form>
`,
page = page
)