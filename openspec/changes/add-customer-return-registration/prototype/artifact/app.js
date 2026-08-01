const stepTitles = ["确认订单", "寄回物流", "设备序列号", "归还照片", "确认提交"];
const state = {
  step: 0,
  devices: [""],
  photos: { exterior: [], serial: [], package: [] }
};

const form = document.querySelector("#return-form");
const steps = [...document.querySelectorAll(".form-step")];
const nextButton = document.querySelector("#next-button");
const backButton = document.querySelector("#back-button");
const deviceList = document.querySelector("#device-list");
const desktopSteps = document.querySelector(".desktop-steps");
const successScreen = document.querySelector("#success-screen");
const toast = document.querySelector("#toast");

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function normalizeSerial(value) {
  return value
    .toUpperCase()
    .replace(/[—–－]/g, "-")
    .replace(/\s+/g, "");
}

function isValidSerial(value) {
  return /^[A-Z0-9]{1,8}(?:-[A-Z0-9]{1,8}){1,4}$/.test(normalizeSerial(value));
}

function buildDesktopSteps() {
  desktopSteps.innerHTML = stepTitles.map((title, index) => `
    <li class="desktop-step" data-desktop-step="${index}">
      <span class="desktop-step__dot">${String(index + 1).padStart(2, "0")}</span>
      <strong>${title}</strong>
    </li>
  `).join("");
}

function updateNavigation() {
  steps.forEach((step, index) => step.classList.toggle("is-active", index === state.step));
  document.querySelectorAll("[data-desktop-step]").forEach((step, index) => {
    step.classList.toggle("is-active", index === state.step);
    step.classList.toggle("is-complete", index < state.step);
    if (index < state.step) step.querySelector(".desktop-step__dot").textContent = "✓";
    else step.querySelector(".desktop-step__dot").textContent = String(index + 1).padStart(2, "0");
  });

  document.querySelector("#mobile-step-label").textContent = `第 ${state.step + 1} 步，共 5 步`;
  document.querySelector("#mobile-step-title").textContent = stepTitles[state.step];
  document.querySelector("#progress-fill").style.width = `${(state.step + 1) * 20}%`;
  backButton.hidden = state.step === 0;
  nextButton.querySelector("span:first-child").textContent = state.step === 4 ? "确认提交" : "下一步";
  nextButton.querySelector("span:last-child").textContent = state.step === 4 ? "✓" : "→";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function setFieldError(element, message) {
  const field = element.closest(".field");
  element.classList.toggle("is-invalid", Boolean(message));
  if (field) field.querySelector(".field-error").textContent = message;
}

function validateStep() {
  let valid = true;

  if (state.step === 0) {
    const orderNo = document.querySelector("#order-no");
    const message = orderNo.value.trim().length < 6 ? "请输入正确的订单号" : "";
    setFieldError(orderNo, message);
    valid = !message;
  }

  if (state.step === 1) {
    const fields = [
      [document.querySelector("#carrier"), "请选择快递公司"],
      [document.querySelector("#shipped-date"), "请选择寄出日期"],
      [document.querySelector("#tracking-no"), "请输入物流单号"]
    ];
    fields.forEach(([element, emptyMessage]) => {
      let message = element.value.trim() ? "" : emptyMessage;
      if (element.id === "tracking-no" && element.value.trim() && element.value.trim().length < 7) {
        message = "物流单号长度不正确";
      }
      setFieldError(element, message);
      if (message) valid = false;
    });
  }

  if (state.step === 2) {
    document.querySelectorAll("[data-device-input]").forEach((input, index) => {
      const value = normalizeSerial(input.value);
      input.value = value;
      state.devices[index] = value;
      const error = input.parentElement.querySelector(".field-error");
      const message = isValidSerial(value)
        ? ""
        : `第 ${index + 1} 台序列号格式应类似 A6-08-4L5H`;
      input.classList.toggle("is-invalid", Boolean(message));
      error.textContent = message;
      if (message) valid = false;
    });
  }

  if (state.step === 3) {
    Object.entries(state.photos).forEach(([category, files]) => {
      const card = document.querySelector(`[data-photo-card="${category}"]`);
      const message = category === "package" || files.length ? "" : "请至少添加 1 张照片";
      card.classList.toggle("is-invalid", Boolean(message));
      card.querySelector(".photo-error").textContent = message;
      if (message) valid = false;
    });
  }

  if (state.step === 4) {
    const agreement = document.querySelector("#agreement");
    const message = agreement.checked ? "" : "请先确认信息真实并同意提交";
    document.querySelector("#agreement-error").textContent = message;
    valid = !message;
  }

  if (!valid) showToast("请先补充当前步骤中的必填信息");
  return valid;
}

function renderDevices() {
  deviceList.innerHTML = state.devices.map((serial, index) => `
    <div class="device-row">
      <span class="device-index">${String(index + 1).padStart(2, "0")}</span>
      <label class="device-input-wrap">
        <input type="text" autocomplete="off" autocapitalize="characters" spellcheck="false"
          maxlength="32" data-device-input="${index}" value="${escapeHtml(serial)}"
          placeholder="例如 A6-08-4L5H">
        <span class="field-error"></span>
      </label>
      <button class="remove-device" type="button" data-remove-device="${index}"
        aria-label="移除第 ${index + 1} 台设备" ${state.devices.length === 1 ? "hidden" : ""}>×</button>
    </div>
  `).join("");

  document.querySelectorAll("[data-device-input]").forEach(input => {
    input.addEventListener("input", event => {
      const normalized = normalizeSerial(event.currentTarget.value);
      event.currentTarget.value = normalized;
      state.devices[Number(event.currentTarget.dataset.deviceInput)] = normalized;
    });
  });
  document.querySelectorAll("[data-remove-device]").forEach(button => {
    button.addEventListener("click", () => {
      state.devices.splice(Number(button.dataset.removeDevice), 1);
      renderDevices();
    });
  });
}

function renderPhotos(category) {
  const card = document.querySelector(`[data-photo-card="${category}"]`);
  card.querySelector(".photo-previews").innerHTML = state.photos[category].map((photo, index) => `
    <div class="photo-preview">
      <img src="${photo.url}" alt="${category} 照片 ${index + 1}">
      <button class="remove-photo" type="button" data-remove-photo="${category}:${index}"
        aria-label="删除照片 ${index + 1}">×</button>
    </div>
  `).join("");
  card.classList.remove("is-invalid");
  card.querySelector(".photo-error").textContent = "";

  card.querySelectorAll("[data-remove-photo]").forEach(button => {
    button.addEventListener("click", () => {
      const [targetCategory, targetIndex] = button.dataset.removePhoto.split(":");
      URL.revokeObjectURL(state.photos[targetCategory][Number(targetIndex)].url);
      state.photos[targetCategory].splice(Number(targetIndex), 1);
      renderPhotos(targetCategory);
    });
  });
}

function buildReview() {
  const photoCount = Object.values(state.photos).reduce((total, files) => total + files.length, 0);
  const packageSummary = state.photos.package.length
    ? `包装 ${state.photos.package.length} 张`
    : "包装未上传（选填）";
  const devices = state.devices.filter(Boolean);
  const hasIssue = form.elements.hasIssue.value === "yes";
  document.querySelector("#review-content").innerHTML = `
    <div class="review-group"><span>订单号</span><strong>${escapeHtml(form.elements.orderNo.value)}</strong></div>
    <div class="review-group"><span>寄回物流</span><strong>
      ${escapeHtml(form.elements.carrier.value)} · ${escapeHtml(form.elements.trackingNo.value)}<br>
      ${escapeHtml(form.elements.shippedDate.value)}
    </strong></div>
    <div class="review-group"><span>设备序列号</span><div class="review-tags">
      ${devices.map(serial => `<span>${escapeHtml(serial)}</span>`).join("")}
    </div></div>
    <div class="review-group"><span>归还照片</span><strong>
      已添加 ${photoCount} 张 · 外观 ${state.photos.exterior.length} 张 ·
      SN ${state.photos.serial.length} 张 · ${packageSummary}
    </strong></div>
    <div class="review-group"><span>设备情况</span><strong>
      ${hasIssue ? escapeHtml(document.querySelector("#issue-description").value || "有异常，未填写详细说明") : "没有发现异常"}
    </strong></div>
  `;
}

function submitDemo() {
  form.hidden = true;
  successScreen.hidden = false;
  document.querySelector("#receipt-no").textContent = `RT-${Date.now().toString().slice(-8)}`;
  document.querySelector("#receipt-tracking").textContent = form.elements.trackingNo.value;
  document.querySelector(".mobile-progress").hidden = true;
  document.querySelectorAll("[data-desktop-step]").forEach(step => {
    step.classList.remove("is-active");
    step.classList.add("is-complete");
    step.querySelector(".desktop-step__dot").textContent = "✓";
  });
  showToast("这是演示提交，未写入数据库");
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("is-visible"), 2600);
}

nextButton.addEventListener("click", () => {
  if (!validateStep()) return;
  if (state.step === 4) {
    submitDemo();
    return;
  }
  state.step += 1;
  if (state.step === 4) buildReview();
  updateNavigation();
});

backButton.addEventListener("click", () => {
  if (state.step === 0) return;
  state.step -= 1;
  updateNavigation();
});

document.querySelector("#add-device").addEventListener("click", () => {
  if (state.devices.length >= 8) {
    showToast("原型最多演示 8 台设备");
    return;
  }
  state.devices.push("");
  renderDevices();
});

document.querySelectorAll("[data-photo-input]").forEach(input => {
  input.addEventListener("change", event => {
    const category = event.currentTarget.dataset.photoInput;
    const files = [...event.currentTarget.files].slice(0, 6 - state.photos[category].length);
    files.forEach(file => state.photos[category].push({
      name: file.name,
      url: URL.createObjectURL(file)
    }));
    event.currentTarget.value = "";
    renderPhotos(category);
  });
});

document.querySelectorAll('input[name="hasIssue"]').forEach(input => {
  input.addEventListener("change", () => {
    document.querySelector(".issue-description").classList.toggle(
      "is-hidden",
      form.elements.hasIssue.value !== "yes"
    );
  });
});

document.querySelector("#issue-description").addEventListener("input", event => {
  document.querySelector("#issue-count").textContent = event.currentTarget.value.length;
});

document.querySelector("[data-fill-tracking]").addEventListener("click", () => {
  document.querySelector("#tracking-no").value = "SFDEMO20260801001";
  document.querySelector("#carrier").value = "顺丰速运";
  document.querySelector("#shipped-date").value = "2026-08-01";
});

document.querySelectorAll("[data-toast]").forEach(button => {
  button.addEventListener("click", () => showToast(button.dataset.toast));
});

document.querySelector("[data-reset-demo]").addEventListener("click", () => window.location.reload());

buildDesktopSteps();
renderDevices();
updateNavigation();
