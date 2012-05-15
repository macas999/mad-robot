package com.oishii.mobile.beans;

import java.util.ArrayList;
import java.util.List;

import com.oishii.mobile.util.TextUtils;

public class OishiiBasket {
	private List<BasketItem> items = new ArrayList<BasketItem>();
	private float currentTotal;
	private float discountedTotal;
	private String deliveryTime;
	private int billingAddressId;
	private int shippingAddressId;
	private boolean isDiscountApplied;
	private float discountPercentage;
	private float discount;

	public float getDiscount() {
		return discount;
	}

	public float getDiscountPercentage() {
		return discountPercentage;
	}

	public void setDiscountPercentage(float discountPercentage) {
		this.discountPercentage = discountPercentage;
		applyDiscount();
	}

	/**
	 * Apply the given discount percentage to the basket
	 */
	private void applyDiscount() {
		discount = (currentTotal * discountPercentage) / 100;
		discount=(float) TextUtils.round(discount, 2);
		discountedTotal = currentTotal - discount;
		discountedTotal = (float) TextUtils.round(discountedTotal, 2);
		isDiscountApplied = true;
	}

	public boolean isDiscountApplied() {
		return isDiscountApplied;
	}

	public void setDiscountApplied(boolean isDiscountApplied) {
		this.isDiscountApplied = isDiscountApplied;
	}

	public float getDiscountedTotal() {
		return discountedTotal;
	}

	public void setDiscountedTotal(float discountedTotal) {
		this.discountedTotal = discountedTotal;
	}

	public int getBillingAddressId() {
		return billingAddressId;
	}

	public void setBillingAddressId(int billingAddressId) {
		this.billingAddressId = billingAddressId;
	}

	public int getShippingAddressId() {
		return shippingAddressId;
	}

	public void setShippingAddressId(int shippingAddressId) {
		this.shippingAddressId = shippingAddressId;
	}

	public String getDeliveryTime() {
		return deliveryTime;
	}

	public void setDeliveryTime(String deliveryTime) {
		this.deliveryTime = deliveryTime;
	}

	public float getCurrentTotal() {
		return currentTotal;
	}

	public List<BasketItem> getBasketItems() {
		return items;
	}

	public void setCurrentTotal(float currentTotal) {
		this.currentTotal = currentTotal;
	}

	private void updateTotal() {
		currentTotal = 0.0f;
		BasketItem item;
		for (int i = 0; i < items.size(); i++) {
			item = items.get(i);
			currentTotal += item.price * item.count;
		}
		currentTotal = (float) TextUtils.round(currentTotal, 2);
		if(isDiscountApplied){
			applyDiscount();
		}
	}

	public void addItem(BasketItem item) {
		items.add(item);
		updateTotal();
	}

	public BasketItem getItem(int index) {
		return items.get(index);
	}

	public void removeItem(int index) {
		items.remove(index);
		updateTotal();
	}

	public void removeAllItems() {
		items.clear();
	}
}
