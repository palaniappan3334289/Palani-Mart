<%@ page contentType="text/html;charset=UTF-8" language="java" %>
</main>

<!-- Footer -->
<footer class="site-footer">
    <div class="footer-top">
        <div class="container footer-grid">
            <div class="footer-col">
                <div class="brand-logo" style="margin-bottom: var(--space-4);">
                    <img class="brand-mark" src="${pageContext.request.contextPath}/assets/img/logo-mark.svg" alt="" width="40" height="40">
                    <span class="brand-name" style="color: #ffffff;">Palani<span style="color: var(--color-zari);">Mart</span></span>
                </div>
                <p class="footer-about">
                    A multi-seller marketplace for buyers and verified sellers. Built with Java Servlets, JSP, HikariCP and H2.
                </p>
            </div>
            <div class="footer-col">
                <h4>Shop</h4>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/products?category=Electronics">Electronics</a></li>
                    <li><a href="${pageContext.request.contextPath}/products?category=Books">Books</a></li>
                    <li><a href="${pageContext.request.contextPath}/products?category=Clothing">Clothing</a></li>
                    <li><a href="${pageContext.request.contextPath}/products?category=Home">Home &amp; kitchen</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Your account</h4>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/products">Browse products</a></li>
                    <li><a href="${pageContext.request.contextPath}/cart">Cart</a></li>
                    <li><a href="${pageContext.request.contextPath}/orders">Track orders</a></li>
                    <li><a href="${pageContext.request.contextPath}/seller/dashboard">Seller hub</a></li>
                </ul>
            </div>
            <div class="footer-col">
                <h4>Our promise</h4>
                <ul>
                    <li><span>7-day returns</span></li>
                    <li><span>Verified sellers</span></li>
                    <li><span>Payments held until delivery</span></li>
                    <li><span>No ads</span></li>
                </ul>
            </div>
        </div>
    </div>
    <div class="footer-bottom">
        <div class="container">
            <p>&copy; 2026 PalaniMart. Academic capstone project.</p>
        </div>
    </div>
</footer>

<!-- Global Component Containers -->
<div id="toast-container" aria-live="polite"></div>
<div id="confirm-modal" class="modal-backdrop" role="dialog" aria-modal="true"></div>
<div id="global-loader"></div>

<script src="${pageContext.request.contextPath}/assets/js/main.js"></script>
</body>
</html>
