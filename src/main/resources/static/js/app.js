document.addEventListener("DOMContentLoaded", function () {
    const btnMinusList = document.querySelectorAll(".btn-minus");
    const btnPlusList = document.querySelectorAll(".btn-plus");
    
    // Nếu không có nút tác động giỏ hàng, ta không tiếp tục cấu hình
    if (!btnMinusList.length && !btnPlusList.length) {
        return;
    }

    const currency = new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND"
    });

    const totalNode = document.getElementById("grandTotal");
    const countNode = document.getElementById("selectedCount");
    const cartDataInput = document.getElementById("cartData");
    const finalAmountNode = document.getElementById("finalAmountText");
    const discountRow = document.getElementById("discountRow");
    const discountAmountNode = document.getElementById("discountAmountText");
    const voucherCodeInput = document.getElementById("voucherCodeInput");
    const applyVoucherBtn = document.getElementById("applyVoucherBtn");
    const voucherFeedback = document.getElementById("voucherFeedback");
    const appliedVoucherCodeInput = document.getElementById("appliedVoucherCode");
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    let appliedDiscount = 0;
    
    let cart = {}; // { itemId: { quantity: 1, price: 200000 } }

    function updateCartUI(id) {
        // Render view (Số lượng trong overlay)
        const qtyDisplays = document.querySelectorAll(`.qty-display[data-id="${id}"]`);
        const qty = cart[id] ? cart[id].quantity : 0;
        qtyDisplays.forEach(el => el.textContent = qty);

        // Giữ sáng thẻ món ăn khi đã có món (Luxury active state)
        const wrapper = document.querySelector(`.dish-card[data-card-id="${id}"]`);
        if(wrapper) {
            if(qty > 0) wrapper.classList.add("has-items");
            else wrapper.classList.remove("has-items");
        }

        // Update thẻ input ẩn cho Backend lấy theo kiểu cũ (qty_xxx)
        const hiddenInput = document.querySelector(`.qty-hidden-${id}`);
        if (hiddenInput) {
            hiddenInput.value = qty;
        }

        let totalItems = 0;
        let totalPrice = 0;
        for (const key in cart) {
            totalItems += cart[key].quantity;
            totalPrice += cart[key].quantity * cart[key].price;
        }

        if (totalNode) {
            totalNode.textContent = currency.format(totalPrice);
        }
        if (finalAmountNode) {
            const finalAmount = Math.max(0, totalPrice - appliedDiscount);
            finalAmountNode.textContent = currency.format(finalAmount);
        }
        if (countNode) {
            countNode.textContent = `${totalItems} món`;
        }

        // Cập nhật trường ẩn cartData theo mảng JSON để hỗ trợ backend
        const cartArray = Object.keys(cart).map(k => ({
            menuItemId: parseInt(k),
            quantity: cart[k].quantity,
            price: cart[k].price
        }));
        if (cartDataInput) cartDataInput.value = JSON.stringify(cartArray);

        // Render Luxury Cart Items (Native CSS)
        const cartItemsContainer = document.getElementById("cartItemsContainer");
        if (cartItemsContainer) {
            if (Object.keys(cart).length === 0) {
                cartItemsContainer.innerHTML = '<p style="font-family: \'Manrope\', sans-serif; font-style: italic; font-size: 0.9rem; color: rgba(243, 228, 205, 0.5); margin: 0;">Chưa có món nào được chọn.</p>';
            } else {
                cartItemsContainer.innerHTML = "";
                for (const key in cart) {
                    const item = cart[key];
                    const nameInput = document.querySelector(`.name-hidden-${key}`);
                    const name = nameInput ? nameInput.value : `Món #${key}`;
                    
                    const itemEl = document.createElement("div");
                    itemEl.style.cssText = "display: flex; justify-content: space-between; align-items: center; font-family: 'Manrope', sans-serif; font-size: 0.95rem; margin-bottom: 8px; color: rgba(243, 228, 205, 0.8); leading-relaxed;";
                    itemEl.innerHTML = `
                        <span style="color: #fde68a; font-weight: 500;">${item.quantity}x ${name}</span>
                        <span style="font-weight: 600; color: #fff; letter-spacing: 0.5px;">${currency.format(item.price * item.quantity)}</span>
                    `;
                    cartItemsContainer.appendChild(itemEl);
                }
            }
        }
        if (cartDataInput) {
            cartDataInput.value = JSON.stringify(cartArray);
        }
    }

    if (applyVoucherBtn && voucherCodeInput) {
        applyVoucherBtn.addEventListener("click", async function () {
            const voucherCode = (voucherCodeInput.value || "").trim();
            if (!voucherCode) {
                if (voucherFeedback) {
                    voucherFeedback.style.display = "block";
                    voucherFeedback.style.color = "#fca5a5";
                    voucherFeedback.textContent = "Vui lòng nhập mã voucher.";
                }
                return;
            }

            try {
                applyVoucherBtn.disabled = true;
                applyVoucherBtn.textContent = "Đang kiểm tra...";
                const headers = {
                    "Accept": "application/json",
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
                };
                if (csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }
                const body = new URLSearchParams({ voucherCode }).toString();
                const response = await fetch("/api/loyalty/validate-voucher", {
                    method: "POST",
                    headers,
                    body
                });
                const data = await response.json();
                if (!response.ok || !data.success) {
                    throw new Error(data.message || "Voucher không hợp lệ.");
                }

                appliedDiscount = Number(data.discountAmount || 50000);
                if (discountRow) {
                    discountRow.style.display = "flex";
                }
                if (discountAmountNode) {
                    discountAmountNode.textContent = `- ${currency.format(appliedDiscount)}`;
                }
                if (appliedVoucherCodeInput) {
                    appliedVoucherCodeInput.value = data.voucherCode || voucherCode;
                }

                const totalAmount = getCurrentTotalAmount();
                if (finalAmountNode) {
                    finalAmountNode.textContent = currency.format(Math.max(0, totalAmount - appliedDiscount));
                }
                if (voucherFeedback) {
                    voucherFeedback.style.display = "block";
                    voucherFeedback.style.color = "#86efac";
                    voucherFeedback.textContent = "Áp dụng voucher thành công.";
                }
                voucherCodeInput.disabled = true;
                applyVoucherBtn.textContent = "Đã áp dụng";
            } catch (error) {
                if (voucherFeedback) {
                    voucherFeedback.style.display = "block";
                    voucherFeedback.style.color = "#fca5a5";
                    voucherFeedback.textContent = error.message || "Không thể áp dụng voucher.";
                }
                applyVoucherBtn.disabled = false;
                applyVoucherBtn.textContent = "Áp dụng";
            }
        });
    }

    btnMinusList.forEach(btn => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            const id = this.dataset.id;
            if (cart[id] && cart[id].quantity > 0) {
                cart[id].quantity--;
                if (cart[id].quantity === 0) {
                    delete cart[id];
                }
                updateCartUI(id);
            }
        });
    });

    btnPlusList.forEach(btn => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            const id = this.dataset.id;
            const price = parseFloat(this.dataset.price || "0");
            if (!cart[id]) {
                cart[id] = { quantity: 0, price: price };
            }
            cart[id].quantity++;
            updateCartUI(id);
        });
    });

    // Xử lý đổi tên Nút submit dựa trên Phương thức thanh toán
    const paymentRadios = document.querySelectorAll('input[name="paymentMethod"]');
    const submitBtn = document.getElementById("submitBookingBtn");
    const bookingForm = document.getElementById("booking-form");

    function getCurrentTotalAmount() {
        if (cartDataInput && cartDataInput.value) {
            try {
                const cartItems = JSON.parse(cartDataInput.value);
                if (Array.isArray(cartItems)) {
                    return cartItems.reduce(function (sum, item) {
                        const quantity = Number(item.quantity || 0);
                        const price = Number(item.price || 0);
                        return sum + (quantity * price);
                    }, 0);
                }
            } catch (error) {
                // Fallback to text parsing below.
            }
        }

        const totalText = (totalNode && totalNode.textContent) ? totalNode.textContent : "0";
        const normalized = totalText.replace(/[^\d]/g, "");
        return Number(normalized || 0);
    }

    paymentRadios.forEach(radio => {
        radio.addEventListener("change", function () {
            if (!submitBtn) return;
            if (this.value === "VNPAY") {
                submitBtn.textContent = "Chuyển đến VNPAY";
            } else {
                submitBtn.textContent = "Xác nhận đặt bàn";
            }
        });
    });

    if (bookingForm) {
        bookingForm.addEventListener("submit", async function (e) {
            const selectedMethod = document.querySelector('input[name="paymentMethod"]:checked');
            if (!selectedMethod || selectedMethod.value !== "VNPAY") {
                return;
            }

            e.preventDefault();
            const totalAmount = getCurrentTotalAmount();
            if (!Number.isFinite(totalAmount) || totalAmount <= 0) {
                window.alert("Vui long chon mon de thanh toan qua VNPAY.");
                return;
            }

            try {
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = "Dang ket noi VNPAY...";
                }
                const formData = new FormData(bookingForm);
                const payload = new URLSearchParams();
                formData.forEach(function (value, key) {
                    payload.append(key, value);
                });

                const response = await fetch("/api/payment/create", {
                    method: "POST",
                    headers: {
                        "Accept": "application/json",
                        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
                    },
                    body: payload.toString()
                });
                if (!response.ok) {
                    throw new Error("Khong the tao URL thanh toan.");
                }
                const data = await response.json();
                if (!data || !data.paymentUrl) {
                    throw new Error("He thong khong tra ve paymentUrl.");
                }
                window.location.href = data.paymentUrl;
            } catch (error) {
                window.alert(error.message || "Co loi khi tao thanh toan VNPAY.");
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = "Chuyen den VNPAY";
                }
            }
        });
    }
});

document.addEventListener("DOMContentLoaded", function () {
    const filterButtons = document.querySelectorAll(".menu-filter-btn");
    const categoryBlocks = document.querySelectorAll(".menu-category-block");
    let currentCategory = "all";
    let transitionToken = 0;

    if (!filterButtons.length || !categoryBlocks.length) {
        return;
    }

    function getCategoryFromHash() {
        const hashValue = window.location.hash.replace("#", "").trim();
        if (!hashValue) {
            return "all";
        }
        const matchedButton = Array.from(filterButtons).find((button) => button.dataset.category === hashValue);
        return matchedButton ? hashValue : "all";
    }

    function syncHash(category) {
        const nextUrl = category === "all"
            ? `${window.location.pathname}${window.location.search}`
            : `${window.location.pathname}${window.location.search}#${category}`;
        window.history.replaceState(null, "", nextUrl);
    }

    function animateVisibleCards() {
        const visibleCards = Array.from(categoryBlocks)
            .filter((block) => !block.classList.contains("is-hidden"))
            .flatMap((block) => Array.from(block.querySelectorAll(".menu-item-card")));

        visibleCards.forEach((card, index) => {
            card.style.animation = "none";
            card.style.opacity = "0";
            card.style.transform = "translateY(14px) scale(0.985)";

            requestAnimationFrame(function () {
                card.style.animation = `menuCardReveal 460ms cubic-bezier(0.22, 1, 0.36, 1) ${index * 55}ms forwards`;
            });
        });
    }

    function clearCardAnimationState(cards) {
        cards.forEach((card) => {
            card.style.animation = "";
            card.style.opacity = "";
            card.style.transform = "";
        });
    }

    function setVisibleCategory(category, updateHash) {
        categoryBlocks.forEach((block) => {
            const blockCategory = block.dataset.category;
            const shouldShow = category === "all" || blockCategory === category;
            block.classList.toggle("is-hidden", !shouldShow);
            block.classList.remove("is-leaving");
        });

        filterButtons.forEach((button) => {
            button.classList.toggle("is-active", button.dataset.category === category);
        });

        if (updateHash) {
            syncHash(category);
        }

        currentCategory = category;
        animateVisibleCards();
    }

    function applyFilter(category, updateHash) {
        if (category === currentCategory) {
            if (updateHash) {
                syncHash(category);
            }
            return;
        }

        const token = ++transitionToken;

        const visibleBlocks = Array.from(categoryBlocks)
            .filter((block) => !block.classList.contains("is-hidden"));
        const outgoingCards = visibleBlocks
            .flatMap((block) => Array.from(block.querySelectorAll(".menu-item-card")));

        visibleBlocks.forEach((block) => block.classList.add("is-leaving"));

        outgoingCards.forEach((card, index) => {
            card.style.animation = `menuCardHide 260ms cubic-bezier(0.22, 1, 0.36, 1) ${index * 36}ms forwards`;
        });

        const totalDelay = outgoingCards.length > 0
            ? 260 + ((outgoingCards.length - 1) * 36)
            : 0;

        window.setTimeout(function () {
            if (token !== transitionToken) {
                return;
            }

            clearCardAnimationState(outgoingCards);
            setVisibleCategory(category, updateHash);
        }, totalDelay);
    }

    filterButtons.forEach((button) => {
        button.addEventListener("click", function () {
            applyFilter(button.dataset.category, true);
        });
    });

    window.addEventListener("hashchange", function () {
        applyFilter(getCategoryFromHash(), false);
    });

    currentCategory = getCategoryFromHash();
    setVisibleCategory(currentCategory, false);
});


