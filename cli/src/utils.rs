/*
 * Copyright 2018 Fluence Labs Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use failure::Error;
use failure::SyncFailure;

use clap::{value_t, ArgMatches};
use console::style;
use ethkey::Secret;
use failure::ResultExt;
use indicatif::{ProgressBar, ProgressStyle};
use reqwest::{Url, UrlError};
use std::fmt::LowerHex;
use std::str::FromStr;
use web3::contract::Options;
use web3::futures::Future;
use web3::transports::Http;
use web3::types::SyncState;
use web3::types::H256;
use web3::Web3;

// Creates progress bar in the console until the work is over
//
// # Arguments
//
// * `msg` - message on progress bar while working in progress
// * `prefix`
// * `finish` - message after work is done
// * `work` - some function to be done
//
// # Examples
// ```
// with_progress("Code uploading...", "1/2", "Code uploaded.", upload_fn)
// ```
// The output while processing:
// ```
// [1/2] ⠙ Code uploading... ---> [00:00:05]
// ```
// The output on the finish:
// ```
// [1/2]   Code uploaded. ---> [00:00:10]
// ```
//
pub fn with_progress<U, F>(msg: &str, prefix: &str, finish: &str, work: F) -> Result<U, Error>
where
    F: FnOnce() -> Result<U, Error>,
{
    let bar = create_progress_bar(prefix, msg);
    let result = work();
    match result {
        Ok(_) => bar.finish_with_message(finish),
        Err(ref e) => {
            let err = e.as_fail().to_string();
            let err = format!("Error: {}", err);
            bar.finish_with_message(err.as_str());
        }
    };
    result
}

const TEMPLATE: &str = "[{prefix:.blue}] {spinner} {msg:.blue} ---> [{elapsed_precise:.blue}]";

// Creates a spinner progress bar, that will be tick at once
fn create_progress_bar(prefix: &str, msg: &str) -> ProgressBar {
    let bar = ProgressBar::new_spinner();

    bar.set_message(msg);
    bar.set_prefix(prefix);
    bar.enable_steady_tick(100);
    bar.set_style(ProgressStyle::default_spinner().template(TEMPLATE));

    bar
}

// print info msg in 'blue: red bold' style
pub fn print_info_msg(msg: &str, important: String) {
    println!("{} {}", style(msg).blue(), style(important).red().bold())
}

// println info msg, hexifying the `id` argument
pub fn print_info_id<T: LowerHex>(msg: &str, id: T) {
    print_info_msg(msg, format!("{:#x}", id));
}

// println info msg, hexifying the `id` argument and right-aligning msg with 10-column width
pub fn print_info_id_short<T: LowerHex>(msg: &str, id: T) {
    println!("{0: >10} {1:#x}", style(msg).blue(), style(id).red().bold())
}

// println tx hash in `blue: red bold` style
pub fn print_tx_hash(tx: H256) {
    print_info_id_short("tx hash:", tx)
}

// Parses URL from the string
pub fn parse_url(url: &str) -> Result<Url, UrlError> {
    match Url::parse(url) {
        Ok(url) => Ok(url),
        Err(error) if error == UrlError::RelativeUrlWithoutBase => {
            let url_with_base = format!("http://{}", url);
            Url::parse(url_with_base.as_str())
        }
        Err(error) => Err(error),
    }
}

pub fn check_sync(web3: &Web3<Http>) -> Result<bool, Error> {
    let sync_state = web3.eth().syncing().wait().map_err(SyncFailure::new)?;
    match sync_state {
        SyncState::Syncing(_) => Ok(true),
        SyncState::NotSyncing => Ok(false),
    }
}

#[allow(unused)]
pub fn options() -> Options {
    Options::default()
}

// Gets the value of option `key` and removes '0x' prefix
pub fn parse_hex_string(args: &ArgMatches, key: &str) -> Result<String, Error> {
    Ok(value_t!(args, key, String).map(|v| v.trim_start_matches("0x").to_string())?)
}

pub fn parse_secret_key(secret_key: Option<&str>) -> Result<Option<Secret>, Error> {
    let secret: Result<Option<Secret>, Error> = parse_hex(secret_key).map_err(Into::into);
    Ok(secret.context("Error parsing secret key")?)
}

pub fn parse_hex<E, T>(hex: Option<&str>) -> Result<Option<T>, E>
where
    T: FromStr<Err = E>,
{
    Ok(
        hex.map(|s| s.trim_start_matches("0x").parse::<T>())
            .map_or(Ok(None), |r| r.map(Some).into())?, // Option<Result> -> Result<Option>
    )
}

pub fn get_opt<E, T>(args: &ArgMatches, key: &str) -> Result<Option<T>, E>
where
    T: FromStr<Err = E>,
{
    let opt: Option<&str> = args.value_of(key);
    let opt: Option<Result<T, _>> = opt.map(|v| v.parse::<T>());
    opt.map_or(Ok(None), |v| v.map(Some))
}

pub fn get_opt_hex<E, T>(args: &ArgMatches, key: &str) -> Result<Option<T>, E>
where
    T: FromStr<Err = E>,
{
    let opt: Option<&str> = args.value_of(key);
    let opt: Option<Result<T, _>> = opt.map(|v| v.trim_start_matches("0x").parse::<T>());
    opt.map_or(Ok(None), |v| v.map(Some))
}
