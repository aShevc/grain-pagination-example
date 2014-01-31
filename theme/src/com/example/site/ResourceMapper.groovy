package com.example.site

import com.sysgears.grain.taglib.Site

/**
 * Change pages urls and extend models.
 */
class ResourceMapper {

    /**
     * Site reference, provides access to site configuration.
     */
    private final Site site

    public ResourceMapper(Site site) {
        this.site = site
    }

    /**
     * This closure is used to transform page URLs and page data models.
     */
    def map = { resources ->

        def refinedResources = resources.findResults(filterPublished).collect { Map resource ->
            setPostsUrls << // passing a resource with date filled to a closure in order to process urls
                    fillDates <<
                    resource
        }.sort { -it.date.time }

        // Capturing all the resources, which location starts with /posts/
        def posts = resources.findAll { it.location =~ /\/posts\/.*/ }

        // Reforming the whole resources list
        refinedResources.inject([]) { List updatedResources, Map page ->
            switch (page.url) {
            // Capturing base page resource
                case '/blog/':
                    // Replacing base page resource with actual paginated pages
                    updatedResources += paginate(posts, 3, page)
                    break
                default:
                    // Keeping other resources as they are
                    updatedResources << page
            }

            updatedResources
        }
    }

    /**
     * Excludes resources with published property set to false,
     * unless it is allowed to show unpublished resources in SiteConfig.
     */
    private def filterPublished = { Map it ->
        (it.published != false || site.show_unpublished) ? it : null
    }

    /**
     * Fills in page `date` and `updated` fields 
     */
    private def fillDates = { Map it ->
        def update = [date: it.date ? Date.parse(site.datetime_format, it.date) : new Date(it.dateCreated as Long),
                updated: it.updated ? Date.parse(site.datetime_format, it.updated) : new Date(it.lastUpdated as Long)]
        it + update
    }

    /**
     * Sets SEO-friendly urls for blog posts.
     */
    private def setPostsUrls = { Map it ->
        // Here we gonna filter the resources, which location starts from /posts/.
        // This way we will catch all the posts posted
        if (it.location =~ /\/posts\/.*/) {
            // Getting formatted information on date and title
            def date = it.date.format('yyyy/MM/dd/')
            def title = it.title.encodeAsSlug()
            // Setting updated url
            it.url = "/blog/$date$title/"
        }

        it
    }

    /**
     * Creates paginated page resources out of given base page and the list of posts.
     *
     * @param pages models of resources , information on which need to be collected to pages
     * @param perPage indicates number of posts included in one page
     * @param basePage model of the base page
     * @return models of paginated pages resources
     */
    private static def paginate(pages, perPage, basePage) {
        def pageUrl = { pageNo -> basePage.url + (pageNo > 1 ? "page/$pageNo/" : '') }
        def splitOnPages = pages.collate(perPage)
        def numPages = splitOnPages.size()
        def pageNo = 0
        splitOnPages.collect { itemsOnPage ->
            def model = [url: (pageUrl(++pageNo)), posts: itemsOnPage]
            if (pageNo > 1) {
                model.prev_page = pageUrl(pageNo - 1)
            }
            if (pageNo < numPages) {
                model.next_page = pageUrl(pageNo + 1)
            }
            // Merging base page with the created resource
            basePage + model
        }
    }
}
