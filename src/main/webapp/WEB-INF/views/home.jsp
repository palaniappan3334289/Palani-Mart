<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="PalaniMart — Everyday marketplace" scope="request" />
<jsp:include page="common/header.jsp" />

<!-- Hero -->
<section class="hero-section">
    <div class="container hero-grid">
        <div>
            <div class="hero-tag">வணக்கம்! Welcome to PalaniMart</div>
            <h1 class="hero-headline">Good things from sellers you can trust</h1>
            <p class="hero-subhead">
                Electronics, books, clothing and home essentials from verified sellers, with secure checkout and easy 7-day returns.
            </p>
            <div class="hero-actions">
                <a href="${pageContext.request.contextPath}/products" class="btn btn-primary btn-lg">Browse products</a>
                <a href="${pageContext.request.contextPath}/register?role=SELLER" class="btn btn-outline btn-lg">Sell on PalaniMart</a>
            </div>
        </div>

        <div>
            <div class="hero-media-card">
                <div style="aspect-ratio: 4/3; background: var(--color-primary-light); border-radius: var(--radius-md); overflow: hidden;">
                    <img src="https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80"
                         alt="Over-ear headphones from the PalaniMart electronics range"
                         style="width: 100%; height: 100%; object-fit: cover;"
                         loading="lazy">
                </div>
                <div style="padding: var(--space-3) var(--space-2) var(--space-1); display: flex; justify-content: space-between; align-items: center; gap: var(--space-3);">
                    <div>
                        <span class="badge badge-gold">Featured</span>
                        <h4 style="margin-top: var(--space-1);">Headphones and audio</h4>
                    </div>
                    <a href="${pageContext.request.contextPath}/products?category=Electronics" class="btn btn-outline btn-sm">Shop electronics</a>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- Why shop here -->
<section class="pillars-section">
    <div class="container pillars-grid">
        <div class="pillar-card">
            <div class="pillar-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3 4.5 6v5.5c0 4.5 3.1 8 7.5 9.5 4.4-1.5 7.5-5 7.5-9.5V6z"/><path d="m8.8 12 2.4 2.4 4-4.4"/></svg></div>
            <div>
                <h4>Verified sellers</h4>
                <p>Every seller is checked before they can list a product.</p>
            </div>
        </div>
        <div class="pillar-card">
            <div class="pillar-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="9" r="5.5"/><path d="m9 13.5-1.5 7 4.5-2.4 4.5 2.4-1.5-7"/></svg></div>
            <div>
                <h4>Authentic products</h4>
                <p>Items come with the seller's own warranty.</p>
            </div>
        </div>
        <div class="pillar-card">
            <div class="pillar-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/></svg></div>
            <div>
                <h4>Protected payments</h4>
                <p>Your payment is held until delivery is confirmed.</p>
            </div>
        </div>
        <div class="pillar-card">
            <div class="pillar-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 4H4v13h4v4l5-4h7z"/></svg></div>
            <div>
                <h4>Help when you need it</h4>
                <p>Support for questions, orders and returns.</p>
            </div>
        </div>
    </div>
</section>

<!-- Categories -->
<section class="container" id="categories" style="padding-top: var(--space-12); padding-bottom: var(--space-6);">
    <div class="section-header">
        <div>
            <h2 class="section-title">Shop by category</h2>
            <p class="section-subtitle">Four places to start</p>
        </div>
        <a href="${pageContext.request.contextPath}/products" class="btn btn-outline btn-sm">All products</a>
    </div>

    <div class="category-grid">
        <a href="${pageContext.request.contextPath}/products?category=Electronics" class="card-category card-category--peacock">
            <div class="category-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 15v-3a8 8 0 0 1 16 0v3"/><rect x="3" y="14" width="4.5" height="6.5" rx="1.5"/><rect x="16.5" y="14" width="4.5" height="6.5" rx="1.5"/></svg></div>
            <h3 class="category-title">Electronics</h3>
            <p>Audio, computing and optics</p>
        </a>
        <a href="${pageContext.request.contextPath}/products?category=Books" class="card-category card-category--rani">
            <div class="category-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3h12a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"/><path d="M8 3v18"/><path d="M12 8h4"/></svg></div>
            <h3 class="category-title">Books</h3>
            <p>Fiction, reference and rare prints</p>
        </a>
        <a href="${pageContext.request.contextPath}/products?category=Clothing" class="card-category card-category--zari">
            <div class="category-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M8 3 3 6l2 4 3-1.2V21h8V8.8L19 10l2-4-5-3a4 4 0 0 1-8 0z"/></svg></div>
            <h3 class="category-title">Clothing</h3>
            <p>Textiles, leather and everyday wear</p>
        </a>
        <a href="${pageContext.request.contextPath}/products?category=Home" class="card-category card-category--indigo">
            <div class="category-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v10h13V10"/><path d="M10 20v-5.5h4V20"/></svg></div>
            <h3 class="category-title">Home</h3>
            <p>Cookware, décor and furniture</p>
        </a>
    </div>
</section>

<!-- Featured products -->
<section class="container" style="padding-top: var(--space-6); padding-bottom: var(--space-12);">
    <div class="section-header">
        <div>
            <h2 class="section-title">Featured products</h2>
            <p class="section-subtitle">Picked by our team</p>
        </div>
        <a href="${pageContext.request.contextPath}/products" class="btn btn-outline btn-sm">Browse all products</a>
    </div>

    <div class="product-grid">
        <c:choose>
            <c:when test="${not empty featuredProducts}">
                <c:forEach var="p" items="${featuredProducts}">
                    <div class="card-product">
                        <div class="card-product-media">
                            <button type="button" class="btn-wishlist" aria-label="Save to Wishlist">&#9825;</button>
                            <c:choose>
                                <c:when test="${not empty p.imageUrl}">
                                    <img src="<c:out value='${p.imageUrl}' />" alt="<c:out value='${p.name}' />" loading="lazy">
                                </c:when>
                                <c:otherwise>
                                    <div class="card-product-placeholder">
                                        <span style="font-size: 1.75rem; margin-bottom: 4px;">&#9733;</span>
                                        <span><c:out value="${p.category}" /></span>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="card-product-body">
                            <span class="card-product-category"><c:out value="${p.category}" /></span>
                            <h3 class="card-product-title">
                                <a href="${pageContext.request.contextPath}/product?id=${p.id}"><c:out value="${p.name}" /></a>
                            </h3>
                            <div class="card-product-rating">
                                <span class="stars">&#9733;&#9733;&#9733;&#9733;&#9733;</span>
                                <span>(Verified)</span>
                            </div>
                            <div class="card-product-price-row">
                                <span class="card-product-price">$<c:out value="${p.price}" /></span>
                                <c:choose>
                                    <c:when test="${p.stockQty > 0}">
                                        <span class="badge badge-in-stock">In Stock</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-out-of-stock">Sold Out</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="card-product-actions">
                                <form action="${pageContext.request.contextPath}/cart/add" method="POST" class="ajax-add-cart-form" style="width: 100%;">
                                    <input type="hidden" name="productId" value="${p.id}">
                                    <input type="hidden" name="quantity" value="1">
                                    <button type="submit" class="btn btn-primary btn-block btn-sm" ${p.stockQty <= 0 ? 'disabled' : ''}>
                                        Add to Cart
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div class="empty-state-icon">&#128230;</div>
                    <h3 class="empty-state-title">Products are on the way</h3>
                    <p class="empty-state-desc">Sellers are adding new listings. Browse the full catalog to see what is live right now.</p>
                    <a href="${pageContext.request.contextPath}/products" class="btn btn-primary">View Products</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</section>

<!-- Seller call-to-action -->
<section class="container" style="padding-bottom: var(--space-12);">
    <div class="promo-banner">
        <div class="promo-content">
            <span class="badge badge-gold" style="margin-bottom: var(--space-2);">For sellers</span>
            <h3>Sell on PalaniMart</h3>
            <p>
                List your products for free, get transparent payouts and see how your shop is doing from one dashboard.
            </p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/register?role=SELLER" class="btn btn-gold btn-lg">Apply as a seller</a>
        </div>
    </div>
</section>

<jsp:include page="common/footer.jsp" />
