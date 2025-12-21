@template.layout.page(
content = @`
    <div class="row">
        <div class="col-md-8 mx-auto">
            <h1 class="mb-4">Анализатор страниц</h1>
            <p class="lead">Бесплатно проверяйте сайты на SEO пригодность</p>

            <form action="/urls" method="post" class="mt-4">
                <div class="input-group mb-3">
                    <input type="text" name="url" class="form-control"
                           placeholder="https://www.example.com" required>
                    <button type="submit" class="btn btn-primary">Проверить</button>
                </div>
            </form>
        </div>
    </div>
`
)