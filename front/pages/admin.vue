<template>
  <div>
    <section id="sidebar">
      <div class="brand-logo">
        <img src="https://devlille.fr/img/logodl.svg" alt="logo" />
      </div>
      <div id="sidebar-nav">
        <ul>
          <li class="active">
            <a href="#">Nouvelle Edition</a>
          </li>
          <li>
            <a href="#">Editions</a>
          </li>
          <li>
            <a href="#">Configuration</a>
          </li>
        </ul>
      </div>
    </section>
    <section id="content">
      <!--   Main Header  -->
      <div class="content">
        <div class="content-header">
          <h1>Créer une nouvelle édition</h1>
        </div>
      </div>
    </section>
  </div>
</template>

<style>
@import url("https://fonts.googleapis.com/css2?family=Anonymous+Pro:ital,wght@0,400;0,700;1,400;1,700&family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&family=Poppins:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap");

:root {
  --masc: #181b22;
  -—gradient: linear-gradient(90deg, rgb(8, 9, 39), rgb(32, 31, 99));
  --color1: #005454;
  --color2: #e9eef4;
  --color3: silver; /*Accemt Color */
  --theFontz: "Popppins", sans-serif;
  --light: black;
  --dark: white;
}

html,
body {
  font-family: "Open Sans", sans-serif;
  height: 100%;
}
body {
  background: #e9eef4;
  height: 100%;
}
img {
  max-width: 100%;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
a {
  text-decoration: none;
}
#header {
  float: left;
  width: 100%;
  background: #ffffff;
  position: relative;
}
.brand-logo {
  float: left;
  background: var(--color1);
  padding: 10px;
}

.header-nav {
  min-height: 64px;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
  box-sizing: border-box;
  background: var(--color1);
}
.menu-button {
  float: left;
  font-size: 29px;
  color: #fff;
  padding: 12px 19px;
}
.nav ul {
  height: 64px;
  float: right;
}
.nav ul li {
  float: left;
  position: relative;
  padding: 11px;
}
.nav > ul > li:first-child {
  border-left: none;
}
.nav ul li a {
  color: #fff;
  padding: 1px;
  float: left;
}
.nav ul li i {
  color: #fff;
}
.nav ul li:hover {
  background: #01a9f0;
  color: #fff;
}
.user-profile {
  float: right;
}
.user-profile > div {
  float: left;
  padding: 20px 8px;
  position: relative;
}
.user-profile i {
  font-size: 1.2em;
  color: #5f6f86;
}
.user-profile i:hover {
  color: #397ac5;
}
.font-icon i:after {
  position: absolute;
  content: "3";
  background: #e74c3c;
  color: #fff;
  font-size: 12px;
  border-radius: 50%;
  width: 10px;
  height: 10px;
  padding: 3px 4px 4px 3px;
  text-align: center;
  top: 12px;
  right: 11px;
}
.font-icon {
  padding: 8px 10px;
}
.font-icon i {
  font-size: 24px;
}
.nav-mail .font-icon i:after {
  background: #2ecc71;
}
div.user-image {
  padding: 9px 5px;
  margin: 0 5px;
  border-left: 1px solid #ccc;
  border-right: 1px solid #ccc;
}
.nav-profile {
  background: ;
}
.nav-profile-image img {
  width: 39px;
  height: 41px;
  border-radius: 50%;
  float: left;
}
.nav-profile-name {
  float: right;
  margin: 11px 7px 8px 14px;
  color: #fff;
}
.nav-profile-name i {
  padding: 0 0 0 11px;
}
.nav-chat i:after {
  display: none;
}
#sidebar {
  overflow: hidden;
  width: 210px;
  height: 100vh;
  float: left;
  background: #2a2d33;
}
#sidebar-nav {
  width: 106%;
  height: calc(100% - 95px);
  padding: 0;
  background: #2a2d33;
  border-right: 1px solid #e0e0e0;
  overflow-y: scroll;
}
#sidebar-nav h2 {
  color: #60636b;
  float: left;
  width: 100%;
  font-size: 0.8em;
  font-family: "Open Sans", sans-serif;
  font-weight: 600;
  text-transform: uppercase;
  padding: 3px 0 2px 20px;
  border-top: 1px solid #4d4c4c;
  box-sizing: border-box;
  margin: 10px 0;
}
#sidebar-nav ul {
}
#sidebar-nav ul li {
}
#sidebar-nav ul li a {
  color: #c2c2c2;
  font-size: 0.95em;
  padding: 15px 20px;
  float: left;
  width: 100%;
  font-weight: 600;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
  box-sizing: border-box;
}
#sidebar-nav ul li:hover a,
#sidebar-nav ul li:hover a i,
#sidebar-nav li.active a,
#sidebar-nav li.active a i {
  color: #333;
}
#sidebar-nav ul li:hover a {
  background: #fff;
  color: #333;
}
#sidebar-nav ul li.active a {
  background: #fff;
  color: #333;
}
#sidebar-nav ul li.active a i {
  background: #fff;
}
#sidebar-nav i {
  padding-right: 8px;
  font-size: 1.3em;
  color: #60636b;
  width: 25px;
  text-align: center;
}
#content {
  float: left;
  width: calc(100% - 210px);
  height: 100%;
  word-wrap: break-word;
  background: #ffffff;
  font-family: Raleway, sans-serif;
}
.content {
  float: left;
  background: #e9eef4;
  width: 100%;
  height: calc(100% - 64px);
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
  box-sizing: border-box;
}
.content-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #fff;
  float: left;
  width: 100%;
  margin-bottom: 15px;
  padding: 15px;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
  box-sizing: border-box;
  border-bottom: 1px solid #ccc;
}
.content-header h1 {
  grid: 1 / 2;
  margin: 0;
  font-weight: normal;
  padding-bottom: 5px;
  font-family: "Inter", sans-serif;
}
</style>
